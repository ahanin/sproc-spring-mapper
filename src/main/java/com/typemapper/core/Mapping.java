package com.typemapper.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.typemapper.annotations.DatabaseField;
import com.typemapper.annotations.Embed;
import com.typemapper.core.fieldMapper.FieldMapper;
import com.typemapper.core.fieldMapper.FieldMapperRegister;
import com.typemapper.exception.NotsupportedTypeException;


public class Mapping {
	
	private final String name;
	private final Field field;
	private boolean embed;
	private Field embedField;
	
	static List<Mapping> getMappingsForClass(@SuppressWarnings("rawtypes") Class clazz) {
		return getMappingsForClass(clazz, false, null);
	}
	
	static List<Mapping> getMappingsForClass(@SuppressWarnings("rawtypes") Class clazz, boolean embed, Field embedField) {
		List<Mapping> result = new ArrayList<Mapping>();
		Field[] fields = clazz.getDeclaredFields();
		for (final Field field : fields) {
			DatabaseField annotation = field.getAnnotation(DatabaseField.class);
			if (annotation != null) {
				result.add(new Mapping(field, annotation.name(), embed, embedField));
			}
			if (!embed) {
				Embed embedAnnotation = field.getAnnotation(Embed.class);
				if (embedAnnotation != null) {
					result.addAll(getMappingsForClass(field.getType(), true, field));
				}
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	Mapping(Field field, String name, boolean embed, final Field embedField) {
		this.name = name;
		this.field = field;
		this.embed = embed;
		this.embedField = embedField;
	}
	
	@SuppressWarnings("unchecked")
	public Class getFieldClass() {
		return field.getType();
	}
	
	@SuppressWarnings("unchecked")
	public Method getSetter(Field field) {
		final String setterName = "set" + capitalize( field.getName() );
		try {
			return field.getDeclaringClass().getDeclaredMethod(setterName, field.getType());
		} catch (Exception e) {
			return null;
		}
	}
	
	public Method getSetter() {
		return getSetter(field);
	}
	
	public Method getGetter(Field field) {
		final String getterName = "get" + capitalize( field.getName() );
		try {
			return field.getDeclaringClass().getDeclaredMethod(getterName);
		} catch (Exception e) {
			return null;
		}

	}
	
	public String getName() {
		return name;
	}
	
	private static final String capitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		if (Character.isUpperCase(name.charAt(0))){
			return name;
		}
		char chars[] = name.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars);
		
	}

	public Field getField() {
		return field;
	}	
	
	public FieldMapper getFieldMapper() throws NotsupportedTypeException {
		FieldMapper mapper = FieldMapperRegister.getMapperForClass(getFieldClass());
		if (mapper == null) {
			throw new NotsupportedTypeException("Could not find mapper for type " + getFieldClass());
		} else {
			return mapper;
		}
	}

	public void map(Object target, Object value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		if (embed) {
			Object embedValue = getEmbedFieldValue(target);
			if (embedValue == null) {
				embedValue = initEmbed(target);
			}
			Method setter = getSetter(); 
			if (setter != null) {
				setter.invoke(embedValue, value);
			} else {
				getField().set(embedValue, value);
			}
		} else {
			Method setter = getSetter(); 
			if (setter != null) {
				setter.invoke(target, value);
			} else {
				getField().set(target, value);
			}
			
		}
	}

	private Object initEmbed(Object target) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Method setter = getSetter(embedField); 
		Object value = embedField.getType().newInstance();
		if (setter != null) {
			setter.invoke(target, value);
		} else {
			getField().set(target, value);
		}
		return value;
		
	}

	private Object getEmbedFieldValue(Object target) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Method setter = getGetter(embedField);
		Object result = null;
		if (setter != null) {
			result = setter.invoke(target);
		} else {
			result = embedField.get(target);
		}
		return result;
	}
}