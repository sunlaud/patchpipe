import javassist.ClassPool
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall

class ClassPatcher {
    CtClass classToPatch

    private ClassPatcher(String classToPatchName) {
        classToPatch = ClassPool.getDefault().get(classToPatchName)
    }

    public static ClassPatcher patchClass(String className) {
        return new ClassPatcher(className)
    }

    public MethodInvocationReplace replaceMethodCall(String classAndMethodName) {
        return new MethodInvocationReplace(classAndMethodName, this)
    }

    public saveTo(String outPath) {
        classToPatch.writeFile(outPath)
    }
}


class MethodInvocationReplace {
    final String calledClassName
    final String calledMethodName
    final ClassPatcher classPatcher

    public MethodInvocationReplace(String classAndMethodName, ClassPatcher classPatcher) {
        String[] methodNameParts = classAndMethodName.split("#")
        calledClassName = methodNameParts[0]
        calledMethodName = methodNameParts[1]
        this.classPatcher = classPatcher
    }

    public ClassPatcher with(String replacement) {
        def editor = new ExprEditor() {
            @Override
            void edit(MethodCall m) {
                if (calledClassName == m.getClassName() && calledMethodName == m.getMethodName()) {
                    println "${calledClassName}#${calledMethodName} --> ${replacement}"
                    m.replace(replacement)
                }
            }
        }
        classPatcher.classToPatch.getDeclaredBehaviors().each { it.instrument(editor) }
        return classPatcher
    }
}




