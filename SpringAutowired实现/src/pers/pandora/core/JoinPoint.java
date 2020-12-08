package pers.pandora.core;

public final class JoinPoint {

    private String methodName;

    private Object[] params;

    private String proxyClassName;

    private Object returnValue;

    private Throwable exception;

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public String getProxyClassName() {
        return proxyClassName;
    }

    public void setProxyClassName(String proxyClassName) {
        this.proxyClassName = proxyClassName;
    }

    @Override
    public String toString() {
        return "JoinPoint{" +
                "methodName='" + methodName + '\'' +
                ", params=" + params +
                ", proxyClassName='" + proxyClassName + '\'' +
                ", returnValue=" + returnValue +
                ", exception='" + exception + '\'' +
                '}';
    }
}
