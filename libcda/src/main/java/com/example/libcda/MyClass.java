package com.example.libcda;

import org.pfsw.text.json.JsonArray;
import org.pfsw.text.json.JsonObject;
import org.pfsw.text.json.JsonParseException;
import org.pfsw.text.json.SimpleJsonParser;
import org.pfsw.tools.cda.CDATool;
import org.pfsw.tools.cda.base.model.ClassInformation;
import org.pfsw.tools.cda.base.model.ClassInformationDependency;
import org.pfsw.tools.cda.base.model.ClassPackage;
import org.pfsw.tools.cda.base.model.IAnalyzableElement;
import org.pfsw.tools.cda.base.model.Workset;
import org.pfsw.tools.cda.base.model.processing.IMutableClassInformationProcessor;
import org.pfsw.tools.cda.core.dependency.analyzer.AClassDependencyDetector;
import org.pfsw.tools.cda.core.dependency.analyzer.CircularDependenciesDetector;
import org.pfsw.tools.cda.core.dependency.analyzer.ClassDependantsDetector;
import org.pfsw.tools.cda.core.dependency.analyzer.DependencyAnalyzer;
import org.pfsw.tools.cda.core.dependency.analyzer.model.ClassesSearchResult;
import org.pfsw.tools.cda.core.dependency.analyzer.model.FindImplementorsOfResult;
import org.pfsw.tools.cda.core.init.WorksetInitializer;
import org.pfsw.tools.cda.core.io.workset.WorksetReader;
import org.pfsw.tools.cda.core.io.workset.WorksetReaderException;
import org.pfsw.tools.cda.core.processing.AsynchronousProcessing;
import org.pfsw.tools.cda.core.processing.IElementsProcessingResultHandler;
import org.pfsw.tools.cda.core.processing.ProcessingResult;
import org.pfsw.tools.cda.ui.Module;
import org.pfsw.tools.cda.ui.main.tree.ProgressCounterDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MyClass {
    private static final boolean showUI = true;
    public static void main(String[] args) {
        if (showUI) {
            CDATool.main(null);
        }else {
            try {
                Workset workset = WorksetReader.readWorkset("/Users/guoqi5/cda_workset.ws");
                WorksetInitializer initializer = new WorksetInitializer(workset);
                initializer.initializeWorksetAndWait(null);
                String path = MyClass.class.getClassLoader().getResource("./class").getPath();
//            String path = MyClass.class.getClassLoader().getResource("./weiyou-sdk").getPath();
//            String string = readJsonFile(new File(path));
//            SimpleJsonParser parser = new SimpleJsonParser();
//            JsonArray jsonArray = parser.parseArray(string);
                List<String> strings = getText(path);
                for (String line : strings) {
                    ClassInformation classInformation = workset.getClassInfo(String.valueOf(line));
                    IMutableClassInformationProcessor<ClassInformation> processor = new ClassDependantsDetector(null, classInformation);
                    ArrayList result = new ArrayList(100);
                    classInformation.getWorkset().processClassInformationObjects(result, processor);
                    int count = 0;
                    for (Object obj : result) {
                        ClassInformation information = (ClassInformation) obj;
                        String jar = information.getClassContainer().getDisplayName();
                        if (jar.contains("weiyou")) {
                            count++;
                        }
                        if (jar.contains("sdk")) {
                            String className = information.getClassName();
                            if (className.contains("$")) {
                                className = className.split("\\$")[0];
                            }
                            if (className.equals(line)) {
                                count++;
                            }
                        }
                    }
                    if (count == result.size() && count > 0) {
                        System.out.println(line);
                    }
                }
            } catch (WorksetReaderException e) {
                e.printStackTrace();
//        } catch (JsonParseException e) {
//            e.printStackTrace();
            }
        }
    }

    protected static List<String> getText(String path) {
        try {
            return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String readJsonFile(File jsonFile) {
        String jsonStr = "";
        try {
            //File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}