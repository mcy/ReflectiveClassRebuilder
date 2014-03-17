 	package com.xorinc.reflectbuilder;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ReflectClassBuilder {

	private static final String tab = "    ";
	private static final String baseTab = " ";
	private static final char endl = '\n';
	private static JFrame frame;
	
	public static final Pattern CLASSNAME = Pattern.compile("([0-9a-zA-Z_]+([.$][0-9a-zA-Z_]+)*)");
	
	public static final Pattern REPEATED_PACKAGE = Pattern.compile("(([0-9a-zA-Z_]+\\.)+)\\1");
	
	public static final Pattern VOID_ARRAY = Pattern.compile("void(\\[\\])+");
		
	private static Map<Character, String> escapes;
		
	static{
		
		escapes = new TreeMap<Character, String>();
		
		escapes.put('\t', "\\t");
		escapes.put('\b', "\\b");
		escapes.put('\n', "\\n");
		escapes.put('\r', "\\r");
		escapes.put('\f', "\\f");
		escapes.put('\'', "\\'");
		escapes.put('"', "\\\"");
		
		List<Byte> escaped = Arrays.asList(Character.CONTROL, Character.FORMAT, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR, Character.PRIVATE_USE, Character.SURROGATE, Character.UNASSIGNED);
		
		
		
		for(int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++){
			
			char c = (char) i;
			
			if(escapes.containsKey(c) || !escaped.contains((byte) Character.getType(c)))
				continue;
						
			String s = Integer.toHexString(i).toUpperCase();
			
			if(s.length() > 4)
				s = s.substring(s.length() - 4);
			
			else if(s.length() < 4){
				
				s = "0000" + s;
				s = s.substring(s.length() - 4);
			}
			
			escapes.put(c, "\\u" + s);
			
		}
				
	}
	
	
	private static ClassLoader loader = ClassLoader.getSystemClassLoader();
	
	public static void main(String... args) {
				
		frame = new JFrame() {

			private static final long serialVersionUID = 1752005301383945075L;
			private final JTextArea console = new JTextArea(50, 150);
			private final JTextField clazzField = new JTextField(20);
			private final JFileChooser fd;
			private File lastFile = new File(new File(System.getProperty("user.home")), System.getProperty("user.name"));
			private List<URL> jars = new ArrayList<URL>();
			
			private void rebuild(){
				
				console.setText("");
				
				if(clazzField.getText().equals("")){
					console.setText(endl + tab + "No class name was entered!");
					return;
				}
								
				try {
					
					console.setText(printClass(forName(clazzField.getText()), baseTab, new TreeSet<Class<?>>(new Comparator<Class<?>>() {

						@Override
						public int compare(Class<?> a, Class<?> b) {

							return a.getName().compareTo(b.getName());
						}
											
					})));
					
					console.select(0, 0);
				}
				catch (ClassNotFoundException ex) {
					
						console.setText(endl + tab + "Class '" + clazzField.getText() + "' does not exist or could not be loaded!");
					
				}
				catch (NoClassDefFoundError ex) {
					
					console.setText(endl + tab + "Class '" + clazzField.getText() + "' references unknown class '" + ex.getMessage().replace('/', '.') + "'! Aborting!");
					
				}
				
				
			}
			
			{
				
				this.setTitle("Xor's Class Signature Rebuilder");
				
				this.setLayout(new GridBagLayout());
				this.setDefaultCloseOperation(EXIT_ON_CLOSE);

				console.setFont(new Font("Courier", Font.PLAIN, 12));	
				console.setEditable(false);
				this.add(new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), newLayout(0, 0, GridBagConstraints.NORTH));
				
				JPanel jp;
				
				jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.LINE_AXIS));
				jp.add(Box.createVerticalStrut(4));
				this.add(jp, newLayout(0, 1, GridBagConstraints.CENTER));
				
				jp = new JPanel();
				jp.add(Box.createHorizontalGlue());
				
				JPanel clazzArea = new JPanel();
				clazzArea.setLayout(new BoxLayout(clazzArea, BoxLayout.LINE_AXIS));
				clazzArea.add(Box.createHorizontalStrut(7));
				clazzArea.add(new JLabel("Class to Rebuild:"));
				clazzArea.add(Box.createHorizontalStrut(7));
				clazzArea.add(clazzField);
				
				clazzField.addKeyListener(new KeyListener() {

					@Override
					public void keyPressed(KeyEvent k) {

						if(new String(new char[] {k.getKeyChar()}).equals(System.getProperty("line.separator")))
							rebuild();
					}

					@Override
					public void keyReleased(KeyEvent k) {}

					@Override
					public void keyTyped(KeyEvent k) {}
					
				});
				
				clazzArea.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				
				jp.add(clazzArea);
				jp.add(Box.createHorizontalGlue());
				
				JPanel buttons = new JPanel();
				
				buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
				
				JButton rebuild = new JButton("Rebuild");
				rebuild.addActionListener(
						
					new ActionListener() {
									
						@Override
						public void actionPerformed(ActionEvent e) {
							
							rebuild();
						}
					
					}
				); 
				
				buttons.add(rebuild);
				
				JButton load = new JButton("Load JAR");
				load.addActionListener(
						
					new ActionListener() {
									
						@Override
						public void actionPerformed(ActionEvent e) {
							
							int status = fd.showOpenDialog(frame);
							if(status == JFileChooser.CANCEL_OPTION)
								return;
							File f = fd.getSelectedFile();
							
							try {
								jars.add(f.toURI().toURL());
								loader = new URLClassLoader(jars.toArray(new URL[jars.size()]), ClassLoader.getSystemClassLoader());
								lastFile = f;
							}
							catch (Exception ex) {
								console.setText(endl + tab + "Error loading JAR '" + f.getAbsolutePath() + "'!");
							}
							
						}
					
					}
				);
				
				buttons.add(load);
				
				JButton reset = new JButton("Reset Classes");
				reset.addActionListener(
						
					new ActionListener() {
									
						@Override
						public void actionPerformed(ActionEvent e) {
							
							jars.clear();
							loader = ClassLoader.getSystemClassLoader();
							
						}
					
					}
				);
				
				buttons.add(reset);
								
				JButton jarlist = new JButton("Loaded JARs");
				jarlist.addActionListener(
						
					new ActionListener() {
									
						@Override
						public void actionPerformed(ActionEvent e) {
							
							console.setText("");
							
							String urls = endl + tab + jars.size() + " JAR files:" + endl;
							
							if(jars.isEmpty())
								urls = endl + tab + "No external JARs are loaded!";
							
							for(URL url : jars){
								
								urls += tab + url.toString().substring(url.toString().lastIndexOf(File.separator) + 1) + endl;
								
							}
							
							console.setText(urls);
							
						}
					
					}
				);
				
				buttons.add(jarlist);
				
				buttons.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				
				jp.add(buttons);
				
				jp.add(Box.createHorizontalGlue());
				this.add(jp, newLayout(0, 2, GridBagConstraints.CENTER));
				
				jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.LINE_AXIS));
				jp.add(Box.createVerticalStrut(4));
				this.add(jp, newLayout(0, 3, GridBagConstraints.CENTER));
				
				fd = new JFileChooser() {

					private static final long serialVersionUID = -6965111853821474365L;
					
					{
						this.removeChoosableFileFilter(this.getFileFilter());
						this.setSelectedFile(lastFile);
						this.addChoosableFileFilter(new FileNameExtensionFilter("JAR Archives", "jar"));
						this.setFileHidingEnabled(true);
						this.setApproveButtonText("Load");
					}
					
				};
						
				this.setResizable(false);
				this.pack();
				this.setLocationRelativeTo(null);
				this.setVisible(true);			
			}
		};
	}
	
	public static String printPackage(Set<Class<?>> classes, String packageName){
		
		String source = "";
		
		Set<Class<?>> sortedClasses = new TreeSet<Class<?>>(new Comparator<Class<?>>() {

			@Override
			public int compare(Class<?> c1, Class<?> c2) {
				
				int result;
				
				int v1 = classType(c1);
				int v2 = classType(c2);
				
				result = v2 - v1;
				
				if(result == 0)
					result = c1.getName().compareTo(c2.getName());
				
				return result;
				
			}
			
		});
		
		sortedClasses.addAll(classes);
		
		source += endl;
		source += baseTab + "// " + "java.version = " + System.getProperty("java.version") + endl;
		source += baseTab + "// " + "java.vm.name = " + System.getProperty("java.vm.name") + endl;
		source += baseTab + "// " + "java.vm.version = " + System.getProperty("java.vm.version") + endl + endl;
			
		source += baseTab + "package " + packageName + endl + endl;
		
		int lastClass = -1;
		
		for(Class<?> clazz : sortedClasses){
			
			int type = classType(clazz);
			
			if(lastClass == -1 || lastClass != type){
						
				source += endl;
				source += baseTab + "// " + typeName(type) + endl;
				source += endl;
				
			}
			
			source += baseTab + clazz.getName() + endl + endl;
			
		}
		
		return source;
	}
	
	public static String printClass(Class<?> clazz, String tabs, Set<Class<?>> imports) throws NoClassDefFoundError{
		
		String source = "";
		
		if(tabs.equals(baseTab)){
			source += endl;
			source += tabs + "// " + "java.version = " + System.getProperty("java.version") + endl;
			source += tabs + "// " + "java.vm.name = " + System.getProperty("java.vm.name") + endl;
			source += tabs + "// " + "java.vm.version = " + System.getProperty("java.vm.version") + endl + endl;
		}
		
		if(clazz.getPackage() != null && tabs.equals(baseTab)){

			source += tabs + "package " + clazz.getPackage().getName()  + ";" + endl;
						
		}
		
		
		if(tabs.equals(baseTab))
			source += "\u0000" + endl;
		
		source += endl;
		
		for(Annotation a : clazz.getAnnotations()){
			
			source += tabs + prepareAnnote(a, imports) + endl;
			
		}
		
		if(!clazz.isAnonymousClass())
			source += tabs + getModifiers(clazz, Element.CLASS, clazz.getModifiers()) + getClassType(clazz) + " " + getGenericName(clazz, true, imports) + getSuperclass(clazz, imports) + getInterfaces(clazz, imports) + " {" + endl;
		
		else
			source += tabs + "new " + getGenericName(clazz.getGenericSuperclass(), addImport(clazz, imports), imports) + "()" + " {" + endl;
		
		if(clazz.getDeclaredFields().length != 0){
			if(!source.endsWith("\n\n"))
				source += endl;
			source += tabs + tab + "// Fields" + endl;
			source += endl;
		}
		
		String lastEnum = "";
		
		if(clazz.isEnum())
			lastEnum = ((Enum<?>) clazz.getEnumConstants()[clazz.getEnumConstants().length - 1]).name();
		
		for(Field f : clazz.getDeclaredFields()){
			
			for(Annotation a : f.getAnnotations()){
				
				source += tabs + tab + prepareAnnote(a, imports) + endl;
				
			}
			
			if(f.isEnumConstant()){
				source += tabs + tab + f.getName() + (lastEnum.equals(f.getName()) ? ";" : ",") + endl;
			}
			else{
				source += tabs + tab + getModifiers(clazz, Element.FIELD, f.getModifiers()) + (getGenericName(f.getGenericType(), addImport(f.getType(), imports), imports)) + " " + f.getName();
				
				getting:
				if(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
					
					f.setAccessible(true);
					
					Object o;
					try {
						o = f.get(null);
					}
					catch (Exception e) {
						break getting;
					}
					
					if(f.getType().isPrimitive() || f.getType().isEnum() || f.getType().isArray() || f.getType() == String.class || f.getType() == Pattern.class || f.getType() == Class.class){			
					
						try {
													
							String str = toStringSpecial(o, imports);
							
							if(str != null)
								source += " = " + str;
						}
						catch (Exception e) {}
					
					}
					
				}
					
				source += ";" + endl;
				
			}
			source += endl;
		}
		
		if(clazz.getDeclaredConstructors().length != 0 && !clazz.isAnonymousClass()){
			if(!source.endsWith("\n\n"))
				source += endl;
			source += tabs + tab + "// Constructors" + endl;
			source += endl;
		}
		
		for(Constructor<?> con : clazz.getDeclaredConstructors()){
			
			if(clazz.isAnonymousClass())
				break;
			
			for(Annotation a : con.getAnnotations()){
				
				source += tabs + tab + prepareAnnote(a, imports) + endl;
				
			}
			
			source += tabs + tab + getModifiers(clazz, Element.CONSTRUCTOR, con.getModifiers());
			
			String generics = getGenerics(con, imports);
			
			if(generics.length() != 0 )
				source += generics + " ";
			
			source += getConstructorName(clazz) + "(";
			
			String params = "";
			
			for(Type t : con.getGenericParameterTypes()){
				
				params += getGenericName(t, addTypeImport(t, imports), imports) + ", ";
				
			}
			
			if(params.length() != 0)				
				params = params.substring(0, params.length() - 2);
			
			source += params + ")";
			
			if(con.getGenericExceptionTypes().length != 0){
				
				String ex = " throws ";
				
				for(Type t : con.getGenericExceptionTypes()){
					
					ex += getGenericName(t, addTypeImport(t, imports), imports) + ", ";
					
				}
				
				ex = ex.substring(0, ex.length() - 2);
			
				source += ex;
			}
			
			source += " {}" + endl;
			source += endl;
		}
		
		
		if(clazz.getDeclaredMethods().length != 0){
			if(!source.endsWith("\n\n"))
				source += endl;
			source += tabs + tab + (clazz.isAnnotation() ? "// Attributes" : "// Methods") + endl;
			source += endl;
		}
		
		for(Method m : clazz.getDeclaredMethods()){
			
			for(Annotation a : m.getAnnotations()){
				
				source += tabs + tab + prepareAnnote(a, imports) + endl;
				
			}
			
			source += tabs + tab + getModifiers(clazz, Element.METHOD, m.getModifiers());
			
			String generics = getGenerics(m, imports);
			
			if(generics.length() != 0 )
				source += generics + " ";
			
			source += getGenericName(m.getGenericReturnType(), addImport(m.getReturnType(), imports), imports) + " ";
			
			source += m.getName() + "(";
			
			String params = "";
			
			for(Type t : m.getGenericParameterTypes()){
				
				params += getGenericName(t, addTypeImport(t, imports), imports) + ", ";
				
			}
			
			if(params.length() != 0)				
				params = params.substring(0, params.length() - 2);
			
			source += params + ")";
			
			if(m.getGenericExceptionTypes().length != 0){
				
				String ex = " throws ";
				
				for(Type t : m.getGenericExceptionTypes()){
					
					ex += getGenericName(t, addTypeImport(t, imports), imports) + ", ";
					
				}
				
				ex = ex.substring(0, ex.length() - 2);
			
				source += ex;
			}
			
			if(clazz.isAnnotation()){
				
				if(m.getDefaultValue() != null){
					
					Object o = m.getDefaultValue();
					
					String str = toStringSpecial(o, imports);
					
					if(str != null)
						source += " default " + str;
										
				}
			}
			
			source += (Modifier.isAbstract(m.getModifiers()) ? ";" : " {}") + endl;
			source += endl;
			
		}
		
		
		if(clazz.getDeclaredClasses().length != 0){
			if(!source.endsWith("\n\n"))
				source += endl;
			source += tabs + tab + "// Nested Classes" + endl;
		}
		
		for(Class<?> inner : clazz.getDeclaredClasses()){
			if(inner.isAnonymousClass())
				continue;			
			source += printClass(inner, tabs + tab, imports);
		}
		
		if(hasAnons(clazz)){
			if(!source.endsWith("\n\n"))
				source += endl;
			source += tabs + tab + "// Anonymous Classes" + endl;
		}
		
		int count = 1;
		
		while(true){
			
			try{
				
				Class<?> anon = Class.forName(clazz.getName() + "$" + count);
				source += printClass(anon, tabs + tab, imports);
				count++;
			}
			catch(ClassNotFoundException e){
				break;
			}
			
		}
		
		source += tabs + "}";
		
		if(source.endsWith("{\n" + tabs + "}"))
			source = source.substring(0, source.length() - 3 - tabs.length()) + "{}";
		
		source += "" + endl;
		
		imports:
		if(tabs.equals(baseTab)){
			
			int importLoc = source.indexOf('\u0000');
			
			if(importLoc < 0){				
				source = source.replace("\u0000", "");
				break imports;
			}
			
			String imp = generateImports(imports, clazz, tabs);
			
			StringBuilder b = new StringBuilder(source);
			
			b.insert(importLoc, imp);
			
			source = b.toString();
			
		}
		
		if(source.endsWith("{\n}"));
			
		
		return source;
	}
	
	public static String getGenericName(Type t, boolean simple, Set<Class<?>> imports){
		
		if(t instanceof Class)
			return getName((Class<?>) t, simple, imports);
		
		else{
						
			String type = t.toString();
			
			Matcher m = CLASSNAME.matcher(type);
			
			while(m.find()){
				
				String s = m.group();
				String s2 = s.replace('$', '.');
				
				int index = 0;
								
				Class<?> c = null;
				
				inner:
				while(true){
					try {
						c = Class.forName(s.substring(index));
						break inner;
					}
					catch (ClassNotFoundException e) {
									
						Matcher m2 = REPEATED_PACKAGE.matcher(s2);
						
						try{
							if(m2.find()){
								
								index += m2.start() + m2.end(1);
								s2 = s2.substring(index);
								
								continue inner;
							}
							else{
								break inner;
							}
						}
						catch (Exception e1) {
							break inner;
						}
					}
				}
				
				if(c != null){
					
					addImport(c, imports);										
					type = type.replaceAll("\\Q" + s + "\\E", c.getSimpleName());
					m.reset(type);
				}
			}
			
			
			return type.replace('$', '.');
		}
			
		
	}
	
	public static String getConstructorName(Class<?> clazz){
				
		if(clazz.isArray())
			return getConstructorName(clazz.getComponentType()) + "[]";
		
		String name = clazz.getSimpleName();	
		
		if(clazz.isAnonymousClass())
			name = clazz.getEnclosingClass().getSimpleName() + ".1"; 
		
		name = name.replace('$', '.');
		
		return name;
		
	}
	
	public static String getName(Class<?> clazz, boolean simple, Set<Class<?>> imports){
		
		if(clazz.isArray())
			return getName(clazz.getComponentType(), simple, imports) + "[]";
		
		String name = simple ? clazz.getSimpleName() : clazz.getName();
				
		if(clazz.isAnonymousClass())
			name = (simple ? clazz.getEnclosingClass().getSimpleName() : clazz.getEnclosingClass().getName()) + ".?"; 
		
		name += getGenerics(clazz, imports);
		
		name = name.replace('$', '.');
		
		return name;
	}
	
	public static String getGenerics(GenericDeclaration dec, Set<Class<?>> imports){
					
		String name = "";
		
		if(dec.getTypeParameters().length != 0){
			
			name += "<";
			
			for(TypeVariable<?> t : dec.getTypeParameters()){
				
				name += t;
				
				if(t.getBounds()[0] != Object.class || t.getBounds().length > 1){
					
					name += " extends ";
					
					for(Type ty : t.getBounds()){
						
						if(ty instanceof Class)
							name += getName((Class<?>) ty, addImport((Class<?>) ty, imports), imports) + " & ";
						else{
														
							String type = ty.toString();
							
							Matcher m = CLASSNAME.matcher(type);
							
							while(m.find()){
								
								String s = m.group();
								
								try {
									Class<?> c = Class.forName(s);
									if(addImport(c, imports)){
										
										type = type.replaceAll(s, c.getSimpleName());
										m.reset(type);
									}									
								}
								catch (Exception e) {
									continue;
								}
								
							}
							
							name += type + " & ";
						}
					}
					
					name = name.substring(0, name.length() - 3);
					
				}
				
				name += ", ";
				
			}
			
			name = name.substring(0, name.length() - 2) + ">";
		
		}	
			
		return name;
			
	}
	
	public static String getSuperclass(Class<?> clazz, Set<Class<?>> imports){
		
		if(clazz.isEnum())
			return "";
		
		if(!clazz.isInterface()){
			if(clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class)
				return " extends " + getGenericName(clazz.getGenericSuperclass(), addImport(clazz, imports), imports);
			
			return "";
		}
		else{
			
			return getInterfaces(clazz, imports).replaceFirst("implements", "extends");
			
		}
		
	}
	
	public static String getInterfaces(Class<?> clazz, Set<Class<?>> imports){
		
		if(clazz.isInterface())
			return "";
		
		Type[] it = clazz.getGenericInterfaces();
		Class<?>[] ic = clazz.getInterfaces();
		
		if(it.length == 0)
			return "";
		
		String s = " implements ";
		
		for(int i = 0; i < it.length; i++){
			
			s += getGenericName(it[i], addImport(ic[i], imports), imports) + ", ";
			
		}
		
		s = s.substring(0, s.length() - 2);
		
		return s;
	}
	
	public static String getClassType(Class<?> clazz){
		
		if(clazz.isAnnotation())
			return "@interface";
		
		else if(clazz.isEnum())
			return "enum";
		
		else if(clazz.isInterface())
			return "interface";
		
		else
			return "class";
	}
	
	public static String printArray(Object array, Set<Class<?>> imports){
		
		if(array == null)
			return "null";
		
		if(!array.getClass().isArray() && !(array.getClass().getComponentType().isEnum() || array.getClass().getComponentType().isPrimitive()))
			return null;
		
		String str = "{";
		
		int length = Array.getLength(array);
		
		for(int i = 0; i < length; i++){
			
			Object o = Array.get(array, i);
			
			str += toStringSpecial(o, imports) + ", ";
			
		}
		
		if(str.length() > 1)
			str = str.substring(0, str.length() - 2);
		
		str += "}";
		
		return str;
		
	}
	
	public static String prepareAnnote(Annotation a, Set<Class<?>> imports){
		
		String s = "@";
		
		s += getName(a.annotationType(), addImport(a.annotationType(), imports), imports) + "(";
		
		Method[] att = a.annotationType().getDeclaredMethods();
		
		if(att.length == 1 && att[0].getName().equals("value")){
			try {
				
				Object o = att[0].invoke(a);
				
				if(!o.equals(att[0].getDefaultValue()))
					s += toStringSpecial(o, imports) + ", ";
			}
			catch (Exception e) {}
		}
		else			
			for(Method m : att){
				
				try {
					Object o = m.invoke(a);
					
					if(m.getDefaultValue() == null || !toStringSpecial(o, imports).equals(toStringSpecial(m.getDefaultValue(), imports)))
						s += m.getName() + " = " + toStringSpecial(o, imports) + ", ";
				}
				catch (Exception e) {}
				
			}
		
		if(s.endsWith(", "))
			s = s.substring(0, s.length() - 2);
		
		s += ")";
		
		if(s.endsWith("()"))
			s = s.substring(0, s.length() - 2);
		
		return s;
	}
	
	public static String getModifiers(Class<?> clazz, Element el, int mod){
		
		String mods = "";
		
		if(Modifier.isPublic(mod) && (!clazz.isInterface() || el != Element.METHOD))
			mods += "public ";
		
		else if(Modifier.isProtected(mod))
			mods += "protected ";
		
		else if(Modifier.isPrivate(mod))
			mods += "private ";
		
		
		if(Modifier.isStatic(mod))
			mods += "static ";
		
		
		if(Modifier.isAbstract(mod) && !clazz.isInterface())
			mods += "abstract ";
		
		else if(Modifier.isFinal(mod) && (!clazz.isEnum() || el != Element.CLASS))
			mods += "final ";
		
		
		if(Modifier.isNative(mod))
			mods += "native ";
		
		
		if(Modifier.isSynchronized(mod))
			mods += "synchronized ";
		
		
		if(Modifier.isStrict(mod))
			mods += "strictfp ";
		
		
		if(Modifier.isTransient(mod))
			mods += "transient ";
		
		
		if(Modifier.isVolatile(mod))
			mods += "volatile ";
		
		return mods;
	}
	
	
	public static boolean addTypeImport(Type t, Set<Class<?>> imports){
		
		if(t instanceof Class)
			return addImport((Class<?>) t, imports);
		else
			return false;
		
	}
	
	public static String toStringSpecial(Object o, Set<Class<?>> imports){
		
		Class<?> cl = o.getClass();
		
		if(cl.isArray()){
			
			String a = printArray(o, imports);

			if(a == null)
				return null;
			else
				return a;
			
		}
		else if(cl.isEnum())
			return o.getClass().getSimpleName() + "." + ((Enum<?>) o).name();
		
		else if(cl == char.class || cl == Character.class){
			
			char c = ((Character) o);
			String s = c + "";
			
			if(c == '\\')
				s = "\\\\";
			
			if(escapes.containsKey(c))
				s = escapes.get(c);
			
			return "'" + s + "'";
			
		}
		
		else if(cl == String.class){
			
			String s = ((String) o);
						
			s = s.replace("\\", "\\\\");
			
			for(Entry<Character, String> e : escapes.entrySet()){
				
				s = s.replace(e.getKey().toString(), e.getValue());							
			}
			
			return '"' + s + '"';
			
		}
		
		else if(cl == Class.class){
			
			String name = getName((Class<?>) o, addImport((Class<?>) o, imports), imports);
			
			if(name.contains("<"))
				name = name.substring(0, name.indexOf('<'));
			
			return name + ".class";
			
		}
		
		else if(cl == Pattern.class){
			
			String s = ((Pattern) o).pattern();
			
			s = s.replace("\\", "\\\\");
			
			for(Entry<Character, String> e : escapes.entrySet()){
				
				s = s.replace(e.getKey().toString(), e.getValue());							
			}
			
			return "Pattern.compile(\"" + s + "\")";
			
		}
		
		else	
			return o.toString();
		
		
	}
	
	public static boolean addImport(Class<?> clazz, Set<Class<?>> imports){
		
		if(clazz.isPrimitive() || clazz.isArray() || clazz.isAnonymousClass())
			return true;
		
		
		if(clazz.getPackage().getName().equals("java.lang"))
			return true;
		
		for(Class<?> c : imports){
			
			if(c.getSimpleName().equals(clazz.getSimpleName()) && c != clazz)
				return false;
			
		}
		
		imports.add(clazz);
		return true;
		
		
	}
	
	public static String generateImports(Collection<Class<?>> imports, Class<?> c, String tabs){
		
		Class<?> lastClass = null;
		
		String i = "" + endl;
		
		mainLoop:
		for(Class<?> clazz : imports){
			
			if(clazz == c || c.getPackage().equals(clazz.getPackage()))
				continue;
			
			Class<?> outer = clazz;
			
			while(outer != null){
				
				if(outer == c)
					continue mainLoop;
				
				outer = outer.getDeclaringClass();
				
			}
			
			if(lastClass != null){
				
				String a = lastClass.getName();
				String b = clazz.getName();
				
				a = a.substring(0, a.indexOf('.'));
				b = b.substring(0, b.indexOf('.'));
				
				if(!a.equals(b))
					i += endl;
				
			}
			
			i += tabs + "import " + clazz.getName() + ";" + endl;
			lastClass = clazz;
			
		}
		
		return i;
	}
	
	public static Class<?> forName(String c) throws ClassNotFoundException {
		
		if(c.equals("boolean"))
			return boolean.class;
		else if(c.equals("byte"))
			return byte.class;
		else if(c.equals("short"))
			return short.class;
		else if(c.equals("int"))
			return int.class;
		else if(c.equals("long"))
			return long.class;
		else if(c.equals("float"))
			return float.class;
		else if(c.equals("double"))
			return double.class;
		else if(c.equals("char"))
			return char.class;
		else if(c.equals("void"))
			return void.class;
		else if(c.endsWith("[]")){
			
			if(VOID_ARRAY.matcher(c).matches())
				return void.class;
			
			return Array.newInstance(forName(c.substring(0, c.length() -2)), 0).getClass();			
		}
		else{
			try{
				return Class.forName("java.lang." + c, true, loader);
			}
			catch (ClassNotFoundException e){
				return Class.forName(c, true, loader);
			}
		}
		
	}
	
	public static boolean hasAnons(Class<?> clazz){
		
		try{
			
			Class.forName(clazz.getName() + "$1");
			return true;
		}
		catch(Throwable t){
			return false;
		}
		
	}
	
	public static int classType(Class<?> clazz){
		
		if(clazz.isAnnotation())
			return 3;
		else if(clazz.isEnum())
			return 2;
		else if(clazz.isInterface())
			return 0;
		else if(clazz.isAssignableFrom(Exception.class))
			return 4;
		else if(clazz.isAssignableFrom(Error.class))
			return 5;
		else
			return 1;
		
	}
	
	public static String typeName(int i){
		
		switch(i){
		
		case 0:
			return "Interfaces";
		case 1:
			return "Classes";
		case 2:
			return "Enums";
		case 3:
			return "Annotaions";
		case 4:
			return "Exceptions";
		case 5:
			return "Errors";
		default:
			throw new IllegalArgumentException();
		}
		
	}
	
	public static GridBagConstraints newLayout(int x, int y, int anchor){
		GridBagConstraints layout = new GridBagConstraints();
		layout.gridx = x;
		layout.gridy = y;
		layout.anchor = anchor;
		return layout;
	}
	
	public static GridBagConstraints newLayout(int x, int y,int sx, int sy, int anchor){
		GridBagConstraints layout = new GridBagConstraints();
		layout.gridx = x;
		layout.gridy = y;
		layout.gridwidth = sx;
		layout.gridheight = sy;
		layout.anchor = anchor;
		return layout;
	}
	
	public static enum Element { CLASS, METHOD, CONSTRUCTOR, FIELD }
}
