/*
 * Copyright 2009 zaichu xiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zcu.xutil.jmx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.*;

import zcu.xutil.utils.Util;
import zcu.xutil.Objutil;



/**
 *
 * @author <a href="mailto:zxiao@yeepay.com">xiao zaichu</a>
 */
public class SimpleDynamic implements DynamicMBean {
	private final Map<String, AttributeInfo> attributes = new HashMap<String, AttributeInfo>();

	private final Map<String, OperateInfo> operations = new HashMap<String, OperateInfo>();

	private final Object instance;

	private final MBeanInfo mbeanInfo;

	public SimpleDynamic(Object aInstance) throws IntrospectionException {
		this.instance = aInstance;
		Class<?> clazz = instance.getClass();
		for (Method m : clazz.getMethods()) {
			MbeanAttribute anno = m.getAnnotation(MbeanAttribute.class);
			if (anno != null) {
				int index = Util.indexGetter(m);
				if(index > 0){
					String property = m.getName().substring(index);
					Method mutator = null;
					if (anno.mutable()) {
						try {
							mutator = clazz.getMethod("set".concat(property), m.getReturnType());
						} catch (NoSuchMethodException e) {
							throw new IntrospectionException("setter not found: ".concat(property));
						}
					}
					attributes.put(Objutil.decapitalize(property), new AttributeInfo(m, mutator));
				}
			} else {
				MbeanOperation opanno = m.getAnnotation(MbeanOperation.class);
				if (opanno != null)
					operations.put(m.getName(), new OperateInfo(opanno.description(), m));
			}
		}
		MBeanAttributeInfo attrInfos[] = new MBeanAttributeInfo[attributes.size()];
		int i = 0;
		for (String name : attributes.keySet()) {
			AttributeInfo attr = attributes.get(name);
			attrInfos[i++] = new MBeanAttributeInfo(name, name, attr.accessor, attr.mutator);
		}
		MBeanOperationInfo opInfos[] = new MBeanOperationInfo[operations.size()];
		i = 0;
		for (String name : operations.keySet()) {
			OperateInfo opt = operations.get(name);
			opInfos[i++] = new MBeanOperationInfo(Objutil.isEmpty(opt.desc) ? name : opt.desc, opt.action);
		}
		String className = clazz.getName();
		mbeanInfo = new MBeanInfo(className, className, attrInfos, null, opInfos, null);
	}
	@Override
	public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
		AttributeInfo attrinfo = attributes.get(name);
		if (attrinfo == null || attrinfo.accessor == null)
			throw new AttributeNotFoundException();
		return invoke(instance, attrinfo.accessor ,null);
	}
	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		AttributeInfo attrinfo = attributes.get(attribute.getName());
		if (attrinfo == null || attrinfo.mutator==null)
			throw new AttributeNotFoundException();
		invoke(instance, attrinfo.mutator ,new Object[]{attribute.getValue()});
	}
	@Override
	public AttributeList getAttributes(String[] attrNames) {
		AttributeList resultList = new AttributeList();
		if (attrNames == null || attrNames.length == 0)
			return resultList;
		for (int i = 0; i < attrNames.length; i++) {
			try {
				Object value = getAttribute(attrNames[i]);
				resultList.add(new Attribute(attrNames[i], value));
			} catch (Exception e) {
				throw Objutil.rethrow(e);
			}
		}
		return resultList;
	}
	@Override
	public AttributeList setAttributes(AttributeList attrList) {
		AttributeList resultList = new AttributeList();
		for (Object item : attrList) {
			Attribute attr = (Attribute) item;
			try {
				setAttribute(attr);
				Object value = getAttribute(attr.getName());
				resultList.add(new Attribute(attr.getName(), value));
			} catch (Exception e) {
				throw Objutil.rethrow(e);
			}
		}
		return resultList;
	}
	@Override
	public Object invoke(String operationName, Object params[], String signature[]) throws MBeanException,
			ReflectionException {
		OperateInfo oi = operations.get(operationName);
		if (oi == null)
			throw new ReflectionException(new NoSuchMethodException(operationName));
		return invoke(instance, oi.action ,params);
	}

	private Object invoke(Object target,Method m ,Object[] params) throws MBeanException, ReflectionException{
		try {
            return m.invoke(target,params);
        } catch (IllegalArgumentException ex) {
            throw new ReflectionException(ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception)
                throw new MBeanException((Exception)cause);
            throw new RuntimeErrorException((Error)cause);
        } catch (IllegalAccessException ex) {
            throw new ReflectionException(ex);
        }
	}
	@Override
	public MBeanInfo getMBeanInfo() {
		return (mbeanInfo);
	}

	private static final class AttributeInfo {
		final Method accessor;

		final Method mutator;

		AttributeInfo(Method getter, Method setter) {
			this.accessor = getter;
			this.mutator = setter;
		}
	}

	private static final class OperateInfo {
		final String desc;

		final Method action;

		OperateInfo(final String description, final Method operator) {
			this.desc = description;
			this.action = operator;
		}
	}
}
