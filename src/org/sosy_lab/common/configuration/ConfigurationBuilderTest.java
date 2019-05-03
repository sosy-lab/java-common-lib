/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.configuration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.common.configuration.converters.TypeConverter;

public class ConfigurationBuilderTest {

  @Test
  public void copyFrom_getInstanceForNewConfiguration() throws InvalidConfigurationException {
    TypeConverter conv = mock(TypeConverter.class);
    Mockito.when(conv.getInstanceForNewConfiguration(any())).thenReturn(conv);

    Configuration config = new ConfigurationBuilder().addConverter(String.class, conv).build();

    Configuration config2 = new ConfigurationBuilder().copyFrom(config).build();

    verify(conv).getInstanceForNewConfiguration(any());
    verifyNoMoreInteractions(conv);
    assertThat(config2.converters).containsEntry(String.class, conv);
    assertThat(config2.converters.entrySet())
        .containsAtLeastElementsIn(Configuration.getDefaultConverters().entrySet());
  }

  @Test
  public void copyFrom_keepPrefix() throws InvalidConfigurationException {
    Configuration base = Configuration.builder().setPrefix("base").build();
    Configuration child = Configuration.copyWithNewPrefix(base, "child");
    Configuration grandchild =
        Configuration.builder().copyFrom(child).setOption("dummy", "test").build();
    Configuration grandgrandchild =
        Configuration.builder().copyFrom(grandchild).setOption("dummy2", "test").build();

    assertThat(grandchild.prefix).isEqualTo("child.");
    assertThat(grandgrandchild.prefix).isEqualTo("child.");
  }

  @Test
  public void addConverter_NoGetInstanceForNewConfiguration() throws InvalidConfigurationException {
    TypeConverter conv1 = mock(TypeConverter.class);
    TypeConverter conv2 = mock(TypeConverter.class);

    Configuration config = new ConfigurationBuilder().addConverter(String.class, conv1).build();

    // Because conv2 explicitly overrides conv1, getInstanceForNewConfiguration should not be called
    Configuration config2 =
        new ConfigurationBuilder().copyFrom(config).addConverter(String.class, conv2).build();

    verifyZeroInteractions(conv1, conv2);
    assertThat(config2.converters).containsEntry(String.class, conv2);
    assertThat(config2.converters.entrySet())
        .containsAtLeastElementsIn(Configuration.getDefaultConverters().entrySet());
  }

  @Test
  public void defaultConverters_getInstanceForNewConfiguration()
      throws InvalidConfigurationException {
    TypeConverter conv = mock(TypeConverter.class);
    Mockito.when(conv.getInstanceForNewConfiguration(any())).thenReturn(conv);

    Configuration config;
    ImmutableMap<Class<?>, TypeConverter> backup =
        ImmutableMap.copyOf(Configuration.getDefaultConverters());
    try {
      Configuration.getDefaultConverters().put(String.class, conv);

      config = new ConfigurationBuilder().build();
    } finally {
      Configuration.getDefaultConverters().clear();
      Configuration.getDefaultConverters().putAll(backup);
    }

    verify(conv).getInstanceForNewConfiguration(any());
    verifyNoMoreInteractions(conv);
    assertThat(config.converters).containsEntry(String.class, conv);
    assertThat(config.converters.entrySet())
        .containsAtLeastElementsIn(Configuration.getDefaultConverters().entrySet());
  }
}
