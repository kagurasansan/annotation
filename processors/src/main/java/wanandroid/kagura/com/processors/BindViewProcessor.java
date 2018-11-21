package wanandroid.kagura.com.processors;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import wanandroid.kagura.com.annotations.BindView;

/**
 * @version $Rev$
 * @auther yinfengma
 * @time 2018/11/20.3:40 PM
 * @des ${TODO}
 * @updateAuthor $Author$
 * @updateData $Author$
 * @updatedes ${TODO}
 */
@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor{

    private Filer mFiler;
    private Messager mMessager;
    private Elements mElementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mMessager = processingEnvironment.getMessager();
        mElementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(BindView.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Map<Element, List<Element>> map = new HashMap<>();
        for (TypeElement typeElement : set) {
            for (Element fieldElement : roundEnvironment.getElementsAnnotatedWith(typeElement)) {
                Element classElement = fieldElement.getEnclosingElement();
                if (fieldElement.getModifiers().contains(Modifier.PRIVATE))
                    throw new RuntimeException("@BindView can't annotate private field with " + fieldElement.getSimpleName()+" in "+classElement.getSimpleName());
                List<Element> list;
                if (map.containsKey(classElement))
                    list = map.get(classElement);
                else {
                    list = new ArrayList<>();
                    map.put(classElement, list);
                }
                list.add(fieldElement);
            }
        }
        for (Map.Entry<Element, List<Element>> entry : map.entrySet()) {
            MethodSpec.Builder constructorBuilder = MethodSpec.methodBuilder("bindViews")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)     //修饰符
                    .addParameter(ClassName.get(entry.getKey().asType()), "obj"); //参数
            for (Element fieldElement : entry.getValue()) {
                constructorBuilder.addStatement("$L.$L = $L.findViewById($L)", "obj", fieldElement.getSimpleName(), "obj", fieldElement.getAnnotation(BindView.class).value());
            }

            TypeSpec helloWorld = TypeSpec.classBuilder(entry.getKey().getSimpleName() + "_Binding")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(constructorBuilder.build())
                    .build();

            JavaFile javaFile = JavaFile.builder(((PackageElement) entry.getKey().getEnclosingElement()).getQualifiedName().toString(), helloWorld)
                    .build();
            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


}


