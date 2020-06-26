// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeErrorException;
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

  protected RuntimeErrorException handleRuntimeErrorException(RuntimeErrorException e) {
    if (e.getTargetError() != null) {
      // Errors are better not hidden in an exception.
      throw e.getTargetError();
    }
    throw e;
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

      } catch (RuntimeErrorException e) {
        throw handleRuntimeErrorException(e);
      } catch (JMException | SecurityException e) {
        logger.logfUserException(
            Level.WARNING,
            e,
            "Error during registration of management interface %s",
            this.getClass().getSimpleName());
        oname = null;
      }
    } else {
      logger.log(
          Level.WARNING, "Cannot register management interface ", this.getClass().getSimpleName());
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
      } catch (RuntimeErrorException e) {
        throw handleRuntimeErrorException(e);
      } catch (JMException | SecurityException e) {
        logger.logException(
            Level.WARNING, e, "Error during unregistration of management interface");
      }
    }
  }
}
