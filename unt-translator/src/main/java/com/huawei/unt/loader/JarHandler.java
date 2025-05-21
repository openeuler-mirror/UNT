package com.huawei.unt.loader;

import com.google.common.collect.ImmutableList;
import com.huawei.unt.model.JavaClass;
import com.huawei.unt.translator.TranslatorException;
import com.huawei.unt.type.UDFType;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.transform.BodyInterceptor;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import sootup.interceptors.Aggregator;
import sootup.interceptors.CastAndReturnInliner;
import sootup.interceptors.ConstantPropagatorAndFolder;
import sootup.interceptors.CopyPropagator;
import sootup.interceptors.EmptySwitchEliminator;
import sootup.interceptors.LocalNameStandardizer;
import sootup.interceptors.LocalSplitter;
import sootup.interceptors.NopEliminator;
import sootup.interceptors.TypeAssigner;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JarHandler {
    private static final List<BodyInterceptor> INTERCEPTORS = ImmutableList.of(new NopEliminator(),
            new EmptySwitchEliminator(),new CastAndReturnInliner(),new LocalSplitter(),new Aggregator(),
            new CopyPropagator(),new ConstantPropagatorAndFolder(),new TypeAssigner(),new LocalNameStandardizer());

    private final JavaView javaView;
    private final TypeHierarchy typeHierarchy;
    private final Map<String, JavaSootClass> allJavaClass = new HashMap<>();

    public JarHandler(String jarPath) {
        JavaView view = getJavaView(jarPath);
        this.typeHierarchy = view.getTypeHierarchy();
        for (JavaSootClass javaSootClass : view.getClasses().collect(Collectors.toList())) {
            allJavaClass.put(javaSootClass.getName(), javaSootClass);
        }
        this.javaView = view;
    }

    public JarHandler(JavaView view) {
        this.typeHierarchy = view.getTypeHierarchy();
        for (JavaSootClass javaSootClass : view.getClasses().collect(Collectors.toList())) {
            allJavaClass.put(javaSootClass.getName(), javaSootClass);
        }
        this.javaView = view;
    }


    public boolean isSubClass(String className, ClassType superClassType) {
        JavaClassType classType = JavaIdentifierFactory.getInstance().getClassType(className);

        if (typeHierarchy.contains(classType)) {
            return typeHierarchy.isSubtype(superClassType, classType);
        } else {
            throw new LoaderException("Can not found class " + className + " in jar");
        }
    }

    public Collection<JavaSootClass> getAllJavaClasses() {

        return allJavaClass.values();
    }

    public JavaClass getJavaClass(ClassType classType, UDFType udfType) {
        String className = classType.getFullyQualifiedName();

        if (allJavaClass.containsKey(className)) {
            JavaSootClass sootClass = allJavaClass.get(className);

            return new JavaClass(sootClass, udfType);
        } else {
            throw new LoaderException("Can not find class in jar: " + classType.getFullyQualifiedName());
        }
    }

    public Optional<JavaSootMethod> tryGetMethod(String className, String methodName, String retType, List<String> params) {
        ClassType classType = javaView.getIdentifierFactory().getClassType(className);

        MethodSignature signature = javaView.getIdentifierFactory()
                .getMethodSignature(classType, methodName, retType, params);

        return javaView.getMethod(signature);
    }

    private static JavaView getJavaView(String jarPath){
        if (jarPath.endsWith(".jar")) {
            File file = new File(jarPath);
            if (file.exists()) {
                AnalysisInputLocation location =
                        new JavaClassPathAnalysisInputLocation(jarPath, SourceType.Library, INTERCEPTORS);
                return new JavaView(location);
            } else {
                throw new TranslatorException("Jar file is not exists");
            }
        } else {
            throw new TranslatorException("Only support translate jar file now");
        }
    }
}
