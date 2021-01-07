package com.miracle.compiler;

import com.google.auto.service.AutoService;
import com.miracle.router.annotation.Router;

import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class RouterProcessor extends AbstractProcessor {
    private Filer mFileUtils;
    private Messager mMessager;
    private Elements elementUtils;
    private Map<String, String[]> routerInfoMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFileUtils = processingEnvironment.getFiler();
        mMessager = processingEnvironment.getMessager();
        elementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> routerElement = roundEnvironment.getElementsAnnotatedWith(Router.class);
        if (routerElement == null || routerElement.size() < 1) {
            return true;
        }
        for (Element element : routerElement) {
            TypeElement typeElement = (TypeElement) element;
            String className = typeElement.getQualifiedName().toString();
            String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            String[] path = element.getAnnotation(Router.class).path();
            mMessager.printMessage(Diagnostic.Kind.NOTE, "process-----------" + className);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "process-----------" + packageName);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "process-----------" + Arrays.toString(path));
            String key = className;
            String[] value = routerInfoMap.get(key);
            if (value == null) {
                routerInfoMap.put(key, path);
            }

        }
        try {
            JavaFileObject jfo = mFileUtils.createSourceFile("com.miracle.router.RouterMap");
            Writer writer = jfo.openWriter();
            writer.write(generateJavaCode());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public String generateJavaCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code. Do not modify!\n");
        builder.append("package ").append("com.miracle.router").append(";\n\n");
        builder.append("import java.util.HashMap;\n");
        builder.append('\n');

        builder.append("public class ").append("RouterMap").append(" {\n");
        builder.append("    ");
        builder.append("private static HashMap<String, String> routerMap = new HashMap<>()").append(";\n");
        builder.append("    ");
        builder.append("static").append(" {\n");
        for (String key : routerInfoMap.keySet()) {
            String[] value = routerInfoMap.get(key);
            for (String val : value) {
                builder.append("    ");
                builder.append("    ");
                builder.append("routerMap.put(\"").append(val).append("\",\"").append(key).append("\");\n");
            }
        }
        builder.append("    ");
        builder.append("}\n");

        generateMethods(builder);
        builder.append('\n');

        builder.append("}\n");
        return builder.toString();
    }

    public void generateMethods(StringBuilder builder) {
        builder.append("    ");
        builder.append("public static String getClassName(String path) {\n");
        builder.append("    ");
        builder.append("    ");
        builder.append("return routerMap.get(path);\n");
        builder.append("    ");
        builder.append("}");
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotationTypes = new LinkedHashSet<>();
        annotationTypes.add(Router.class.getCanonicalName());
        return annotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}