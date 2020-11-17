package per.pandora.fullCopy.core;

import per.pandora.ObjectStrategyFactory;

public abstract class ObjectCopyStrategyFactory implements ObjectStrategyFactory {

    @Override
    public <T> T handle(T t) {
        return copy(t);
    }

    public abstract <T> T copy(T t);
}
