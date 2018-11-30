/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;

/**
 * Abstract class that encapsulates the registration of an MBean with the {@link MBeanServer}.
 * Exceptions that occur are swallowed and logged.
 *
 * <p>This class is not thread-safe.
 */
public abstract class AbstractMBean {

  private static final @Nullable MBeanServer MBEAN_SERVER = getMBeanServer();

  @Nullable
  private static MBeanServer getMBeanServer() {
    try {
      // wrap this call in method so that an exception does not prevent the
      // whole program from continuing
      return ManagementFactory.getPlatformMBeanServer();
    } catch (SecurityException e) {
      // ignore exception because we cannot handle it here
      return null;
    }
  }

  private @Nullable ObjectName oname = null;
  private final LogManager logger;

  protected AbstractMBean(String name, LogManager logger) {
    this.logger = checkNotNull(logger);

    if (MBEAN_SERVER != null) {
      try {
        oname = new ObjectName(checkNotNull(name));
      } catch (MalformedObjectNameException e) {
        logger.logException(
            Level.WARNING, e, "Invalid object name specified for management interface");
      }
    }
  }

  /**
   * Register this instance at the platform MBeanServer. Swallows all checked exceptions that might
   * occur and logs them.
   */
  public void register() {
    if (MBEAN_SERVER != null && oname != null) {
      try {

        // if there is already an existing MBean with the same name, try to unregister it
        if (MBEAN_SERVER.isRegistered(oname)) {
          MBEAN_SERVER.unregisterMBean(oname);

          assert !MBEAN_SERVER.isRegistered(oname);
        }

        // now register our instance
        MBEAN_SERVER.registerMBean(this, oname);

      } catch (JMException | SecurityException e) {
        logger.logException(Level.WARNING, e, "Error during registration of management interface");
        oname = null;
      }
    } else {
      logger.log(Level.WARNING, "Cannot register management interface");
    }
  }

  /**
   * Unregister this instance. May be called even if registration was not successful (does nothing
   * in this case).
   */
  public void unregister() {
    if (MBEAN_SERVER != null && oname != null) {
      try {
        MBEAN_SERVER.unregisterMBean(oname);
      } catch (JMException | SecurityException e) {
        logger.logException(
            Level.WARNING, e, "Error during unregistration of management interface");
      }
    }
  }
}
