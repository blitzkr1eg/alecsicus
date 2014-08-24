package com.yell.instrumentation;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;

import com.yell.annotation.Yell;
import com.yell.annotation.YellChar;
import com.yell.annotation.YellInt;
import com.yell.annotation.YellString;

//this class will be registered with instrumentation agent
public class DurationTransformer implements ClassFileTransformer {
	public byte[] transform(ClassLoader loader, String className,
			Class classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		byte[] byteCode = classfileBuffer;

		System.out.println("Instrumenting......");
		try {
			ClassPool classPool = ClassPool.getDefault();
			classPool.insertClassPath(new ClassClassPath(this.getClass()));
			CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(
					classfileBuffer));
			if (containsYellClassAnnotations(ctClass)) {
				CtMethod[] methods = ctClass.getDeclaredMethods();
				for (CtMethod method : methods) {
					/*YellString yellString = containsYellString(method,
							YellString.class);
					if (yellString != null) {
						System.out.println("Found @Yell on method "
								+ method.getName() + " ()");*/
					updateWithAlert(ctClass, method/*, yellString*/);
					//}
				}
				byteCode = ctClass.toBytecode();
				ctClass.detach();
				System.out.println("Instrumentation complete.");
			}
		} catch (Throwable ex) {
			System.out.println("Exception: " + ex);
			ex.printStackTrace();

		}

		return byteCode;
	}


	private boolean containsYellClassAnnotations(CtClass ctClass) {
		boolean containsYellClassAnnotations = false;
		Object[] availableAnnotations = ctClass.getAvailableAnnotations();
		for (Object annotation : availableAnnotations) {
			if (annotation instanceof Yell) {
				return true;
			}
		}
		return containsYellClassAnnotations;
	}

	private YellString containsYellString(CtMethod method,
			Class<YellString> class1) {
		Object[] availableAnnotations = method.getAvailableAnnotations();
		for (Object annotation : availableAnnotations) {
			if (annotation instanceof YellString) {
				return (YellString) annotation;
			}
		}
		return null;
	}

	private void updateWithAlert(CtClass ctClass, CtMethod method) {
		Object[] availableAnnotations = method.getAvailableAnnotations();
		for (Object annotation : availableAnnotations) {
		/*	if (annotation instanceof YellChar) {
				updateWithCharAlerter(ctClass, method,(YellChar) annotation);
			} /*else if (annotation instanceof YellCustom){
				updateWithCustomAlerter(ctClass, method,(YellCustom) annotation);
			} else*/ if (annotation instanceof YellInt){
				updateWithIntAlerter(ctClass, method,(YellInt) annotation);
			} /*else if (annotation instanceof YellIntArray){
				updateWithIntArrayAlerter(ctClass, method,(YellIntArray) annotation);
			} else if (annotation instanceof YellRegexPattern){
				updateWithRegexPatternAlerter(ctClass, method,(YellRegexPattern) annotation);
			} else if (annotation instanceof YellString){
				updateWithStringAlerter(ctClass, method,(YellString) annotation);
			} else if (annotation instanceof YellStringArray){
				updateWithStringArrayAlerter(ctClass, method,(YellStringArray) annotation);
			} else if (annotation instanceof YellThrowsCheckedException){
				updateWithIntAlerter(ctClass, method,(YellThrowsCheckedException) annotation);
			} else if (annotation instanceof YellThrowsUncheckedException){
				updateWithThrowsUncheckedExceptionAlerter(ctClass, method,(YellThrowsUncheckedException) annotation);
			}*/
		}
		
	}
	
	
	private void updateWithIntAlerter(CtClass cc, CtMethod m, YellInt yellInt) {
		try {
			if (m.getReturnType().getName() == int.class.getName()) {

				String name = m.getName();
				makeMethodPrivate(m);

				if(yellInt.times() > 0){
					CtMethod newMethod = new CtMethod(m.getReturnType(), name, m.getParameterTypes(), cc);
					newMethod.setModifiers(AccessFlag.clear(AccessFlag.setPublic(newMethod.getModifiers()), AccessFlag.ABSTRACT));

				if (yellInt.times() == 1) {
					
					newMethod.setBody( " { int result =  this." + m.getName()+ "();"
							+ "if (result == " + yellInt.result() + ") {"
							+ "System.out.println(\"intercepted result is: \" + result);\n"
							+ "com.yell.webservice.YellMessage yellMessage = new com.yell.webservice.YellMessage(\""+yellInt.message()+"\",\""+ cc.getName() +"\",\""+ name +"\");\n"
							+ "com.yell.webservice.Service.yellMessageList.add(yellMessage);\n" 
							+ "} " 
							+ " return result; } ");
						
					} else if (yellInt.times() > 1) {
                        String queueName = "queueIntTimes";
						createQueue(cc, queueName);
				
						newMethod.setBody( " { int result =  this." + m.getName()+ "();"
								+ "if (result == " + yellInt.result() + ") {"
								+ "System.out.println(\"intercepted result is: \" + result);\n"
								+ "if (linkedList.size() == "+ (yellInt.times()- 1) +") {"
								+ "com.yell.webservice.YellMessage yellMessage = new com.yell.webservice.YellMessage(\""+yellInt.message()+"\",\""+ cc.getName() +"\",\""+ name +"\");\n"
								+ "com.yell.webservice.Service.yellMessageList.add(yellMessage);\n" 
								+ "for (int i = 0; i < "+(yellInt.times()- 1)+"; i++) {"
								+ "queueIntTimes.poll();"
								+ "}"	
								+ "} else {"
								+ "com.yell.webservice.YellMessage yellMessage = new com.yell.webservice.YellMessage(\""+yellInt.message()+"\",\""+ cc.getName() +"\",\""+ name +"\");\n"
								+ "queueIntTimes.add(yellMessage);\n"
								+ "} }"
								+ " return result; } ");
					}
					cc.addMethod(newMethod);
				}
			}
		} catch (CannotCompileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void makeMethodPrivate(CtMethod m) {
		if (AccessFlag.isPublic(m.getModifiers())) {
			m.setName(m.getName() + "Wrapped");
			m.setModifiers(AccessFlag.PRIVATE);
		}
		
	}
	
	private void createQueue(CtClass cc, String name) {
		try {
			cc.getDeclaredField(name);
		} catch (NotFoundException e) {
			try {
				CtClass arrListClazz = ClassPool.getDefault().get("java.util.Queue");
				CtField f = new CtField(arrListClazz, name, cc);

				f.setModifiers(Modifier.STATIC | Modifier.PRIVATE);
				cc.addField(f, CtField.Initializer.byNew(ClassPool.getDefault().get("java.util.LinkedList")));
			} catch (CannotCompileException e1) {
				System.out.println("Can not create Queue " + name);
				e1.printStackTrace();
			} catch (NotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

}