package per.pandora;


import java.util.logging.Logger;

public interface ObjectStrategyFactory {

    public <T> T handle(T t);
}
