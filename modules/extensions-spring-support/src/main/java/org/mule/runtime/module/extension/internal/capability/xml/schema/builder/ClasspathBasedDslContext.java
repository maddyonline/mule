/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.lang.String.format;
import static org.mule.runtime.core.util.annotation.AnnotationUtils.getAnnotation;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslResolvingContext;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * Implementation of {@link DslResolvingContext} that
 * scans the Classpath looking for the {@link Class} associated to the
 * provided Extension {@code name}
 *
 * @since 4.0
 */
public class ClasspathBasedDslContext implements DslResolvingContext {

  private final ClassLoader classLoader;
  private final Map<String, Class<?>> extensionsByName = new HashMap<>();
  private final Map<String, ExtensionModel> resolvedModels = new HashMap<>();
  private final ExtensionFactory extensionFactory =
      new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());

  public ClasspathBasedDslContext(ClassLoader classLoader) {
    this.classLoader = classLoader;
    findExtensionsInClasspath();
  }

  @Override
  public Optional<ExtensionModel> getExtension(String name) {

    if (!resolvedModels.containsKey(name) && extensionsByName.containsKey(name)) {
      Describer describer = new AnnotationsBasedDescriber(extensionsByName.get(name), new StaticVersionResolver("0.1"));
      DescribingContext context = new DefaultDescribingContext(classLoader);
      resolvedModels.put(name, extensionFactory.createFrom(describer.describe(context), context));
    }

    return Optional.ofNullable(resolvedModels.get(name));
  }


  private void findExtensionsInClasspath() {

    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

    // Exclude some common classes
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile("com\\.sun\\..*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile("sun\\..*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile("jdk\\..*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile("java.*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*springframework.*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile("com\\.google\\..*")));
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile("org\\.apache\\..*")));

    // Exclude mule
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile("org.mule.runtime.*")));
    // Exclude inner classes
    scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*\\$.*")));

    // Look for Extensions
    scanner.addIncludeFilter(new AnnotationTypeFilter(Extension.class));
    scanner.setResourceLoader(new PathMatchingResourcePatternResolver(classLoader));

    System.out.println(System.currentTimeMillis());
    Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents("");
    System.out.println(System.currentTimeMillis());
    System.out.println("CANDIDATES " + Arrays.toString(candidateComponents.toArray()));
    for (BeanDefinition bd : candidateComponents) {
      final String className = bd.getBeanClassName();
      try {
        final Class<?> extensionType = ClassUtils.getClass(className);
        Optional<Extension> annotation = getAnnotation(extensionType, Extension.class);
        if (!annotation.isPresent()) {
          throw new IllegalArgumentException(format("Extension [%s] not found in classpath", className));
        }

        extensionsByName.put(annotation.get().name(), extensionType);
      } catch (ClassNotFoundException ignored) {

        //throw new IllegalArgumentException(format("Extension [%s] not found in classpath", className));
      }
    }
  }
}
