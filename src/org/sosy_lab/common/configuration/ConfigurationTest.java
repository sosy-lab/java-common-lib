/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.common.configuration;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EnumSet;

import org.junit.Test;

import com.google.common.base.Throwables;

public class ConfigurationTest {

  private static enum TestEnum {
    E1, E2, E3;
  }

  @Options
  private static class TestEnumSetOptions {

    @Option(description="Test injection of EnumSet")
    private EnumSet<? extends TestEnum> values = EnumSet.of(TestEnum.E1, TestEnum.E3);
  }

  @Test
  public void testEnumSet() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("values", "E3, E2")
                           .build();

    TestEnumSetOptions options = new TestEnumSetOptions();
    config.inject(options);
    assertEquals(EnumSet.of(TestEnum.E2, TestEnum.E3), options.values);
  }

  @Test
  public void testEnumSetDefault() throws InvalidConfigurationException {
    testDefault(TestEnumSetOptions.class);
  }

  /**
   * This is parameterized test case that checks whether the injection with
   * a default configuration does not change the value of the fields.
   * @param clsWithOptions A class with some declared options and a default constructor.
   */
  private void testDefault(Class<?> clsWithOptions) throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();

    try {
      Constructor<?> constructor = clsWithOptions.getDeclaredConstructor(new Class<?>[0]);
      constructor.setAccessible(true);

      Object injectedInstance = constructor.newInstance();
      config.inject(injectedInstance);

      Object defaultInstance = constructor.newInstance();

      for (Field field : clsWithOptions.getFields()) {
        field.setAccessible(true);
        Object injectedValue = field.get(injectedInstance);
        Object defaultValue = field.get(defaultInstance);

        assertEquals(defaultValue, injectedValue);
      }

    } catch (ReflectiveOperationException e) {
      Throwables.propagate(e);
    }
  }
}
