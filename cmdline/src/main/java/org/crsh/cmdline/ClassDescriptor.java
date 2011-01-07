/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.cmdline;

import org.crsh.cmdline.binding.ClassFieldBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class ClassDescriptor<T> extends CommandDescriptor<T, ClassFieldBinding> {

  /** . */
  private static final Logger log = LoggerFactory.getLogger(ClassDescriptor.class);

  /** . */
  private final Class<T> type;

  /** . */
  private final Map<String, MethodDescriptor<T>> methodMap;

  public ClassDescriptor(Class<T> type) throws IntrospectionException {
    super(
      type.getSimpleName().toLowerCase(),
      info(type.getAnnotations()),
      parameters(type));

    //
    Set<String> optionNames = getOptionNames();

    // Make sure we can add it
    Map<String, MethodDescriptor<T>> methodMap = new LinkedHashMap<String, MethodDescriptor<T>>();
    for (MethodDescriptor<T> method : commands(type)) {

      Set<String> diff = new HashSet<String>(optionNames);
      diff.retainAll(method.getOptionNames());
      if (diff.size() > 0) {
        throw new IntrospectionException("Cannot add method " + method.getName() + " because it has common "
        + " options with its class: " + diff);
      }

      //
      methodMap.put(method.getName(), method);
    }

    //
    this.methodMap = methodMap;
    this.type = type;
  }

  @Override
  public Class<T> getType() {
    return type;
  }

  public Iterable<MethodDescriptor<T>> getMethods() {
    return methodMap.values();
  }

  public MethodDescriptor<T> getMethod(String name) {
    return methodMap.get(name);
  }

  @Override
  public void printUsage(PrintWriter writer) {
    if (methodMap.size() == 1) {
      methodMap.values().iterator().next().printUsage(writer, false);
    } else {
      writer.append("usage: ").append(getName());
      for (OptionDescriptor<?> option : getOptions()) {
        option.printUsage(writer);
      }
      writer.append(" COMMAND [ARGS]\n\n");
      writer.append("The most commonly used ").append(getName()).append(" commands are:\n");
      String formatString = "   %1$-16s %2$s\n";
      for (MethodDescriptor<T> method : getMethods()) {
        Formatter formatter = new Formatter(writer);
        formatter.format(formatString, method.getName(), method.getDescription());
      }
    }
  }

  public void printMan(PrintWriter writer) {
    if (methodMap.size() == 1) {
      methodMap.values().iterator().next().printMan(writer, false);
    } else {
      writer.print("todo");
    }
  }

  private static List<ParameterDescriptor<ClassFieldBinding>> parameters(Class<?> introspected) throws IntrospectionException {
    List<ParameterDescriptor<ClassFieldBinding>> parameters;
    Class<?> superIntrospected = introspected.getSuperclass();
    if (superIntrospected == null) {
      parameters = new ArrayList<ParameterDescriptor<ClassFieldBinding>>();
    } else {
      parameters = parameters(superIntrospected);
      for (Field f : introspected.getDeclaredFields()) {
        Tuple tuple = get(f.getAnnotations());
        ClassFieldBinding binding = new ClassFieldBinding(f);
        ParameterDescriptor<ClassFieldBinding> parameter = create(
          binding,
          f.getGenericType(),
          tuple.argumentAnn,
          tuple.optionAnn,
          tuple.descriptionAnn,
          tuple.ann);
        if (parameter != null) {
          parameters.add(parameter);
        }
      }
    }
    return parameters;
  }

  private List<MethodDescriptor<T>> commands(Class<?> introspected) throws IntrospectionException {
    List<MethodDescriptor<T>> commands;
    Class<?> superIntrospected = introspected.getSuperclass();
    if (superIntrospected == null) {
      commands = new ArrayList<MethodDescriptor<T>>();
    } else {
      commands = commands(superIntrospected);
      for (Method m : introspected.getDeclaredMethods()) {
        MethodDescriptor<T> mDesc = MethodDescriptor.create(this, m);
        if (mDesc != null) {
          commands.add(mDesc);
        }
      }
    }
    return commands;
  }
}