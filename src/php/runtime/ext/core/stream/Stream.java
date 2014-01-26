package php.runtime.ext.core.stream;

import php.runtime.Memory;
import php.runtime.common.HintType;
import php.runtime.common.Messages;
import php.runtime.common.Modifier;
import php.runtime.env.Environment;
import php.runtime.env.TraceInfo;
import php.runtime.exceptions.CriticalException;
import php.runtime.lang.BaseObject;
import php.runtime.lang.Resource;
import php.runtime.memory.ObjectMemory;
import php.runtime.memory.StringMemory;
import php.runtime.reflection.ClassEntity;

import static php.runtime.annotation.Reflection.*;

@Name(Stream.CLASS_NAME)
@Signature({
        @Arg(value = "path", modifier = Modifier.PRIVATE, readOnly = true, type = HintType.STRING),
        @Arg(value = "mode", modifier = Modifier.PRIVATE, readOnly = true)
})
abstract public class Stream extends BaseObject implements Resource {

    @Ignore
    public final static String CLASS_NAME = "php\\io\\Stream";

    private String path;
    private String mode;
    private Memory context = Memory.NULL;

    public Stream(Environment env, ClassEntity clazz) {
        super(env, clazz);
    }

    protected static void exception(Environment env, String message, Object... args){
        PHP_IOException exception = new PHP_IOException(env, env.fetchClass("php\\io\\IOException"));
        exception.__construct(env, new StringMemory(String.format(message, args)));
        env.__throwException(exception);
    }

    @Signature({@Arg("path"), @Arg(value = "mode", optional = @Optional("NULL"))})
    public Memory __construct(Environment env, Memory... args){
        setPath(args[0].toString());
        setMode(args[1].isNull() ? null : args[1].toString());

        return Memory.NULL;
    }

    @Signature({@Arg("value")})
    public Memory setContext(Environment env, Memory... args){
        context = args[0];
        return Memory.NULL;
    }

    @Signature
    public Memory getContext(Environment env, Memory... args){
        return context;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        __class__.setProperty(this, "path", new StringMemory(path));
        this.path = path;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        __class__.setProperty(this, "mode", mode == null ? null : new StringMemory(mode));
        this.mode = mode;
    }

    @Signature({@Arg("value"), @Arg(value = "length", optional = @Optional("NULL"))})
    abstract public Memory write(Environment env, Memory... args);

    @Signature({@Arg(value = "length")})
    abstract public Memory read(Environment env, Memory... args);

    @Signature
    abstract public Memory readFully(Environment env, Memory... args);

    @Signature
    abstract public Memory eof(Environment env, Memory... args);

    @Signature(@Arg("position"))
    abstract public Memory seek(Environment env, Memory... args);

    @Signature
    abstract public Memory getPosition(Environment env, Memory... args);

    @Signature
    abstract public Memory close(Environment env, Memory... args);

    public static Stream create(Environment env, TraceInfo trace, String path, String mode) throws Throwable {
        String protocol = "file";
        int pos = path.indexOf("://");
        if (pos > -1) {
            protocol = path.substring(0, pos);
            path = path.substring(pos + 3);
        }

        ClassEntity classEntity = env.getUserValue(Stream.class.getName() + "#" + protocol, ClassEntity.class);
        if (classEntity == null){
            return null;
        }

        return (Stream)classEntity.newObject(env, trace, true, new StringMemory(path), new StringMemory(mode));
    }

    @Signature({@Arg("path"), @Arg("mode")})
    public static Memory create(Environment env, Memory... args) throws Throwable {
        String path = args[0].toString();

        String protocol = "file";
        int pos = path.indexOf("://");
        if (pos > -1) {
            protocol = path.substring(0, pos);
            path = path.substring(pos + 3);
        }

        ClassEntity classEntity = env.getUserValue(Stream.class.getName() + "#" + protocol, ClassEntity.class);
        if (classEntity == null){
            exception(env, "Unregistered protocol - %s://", protocol);
            return Memory.NULL;
        }

        return new ObjectMemory(
                classEntity.newObject(env, env.trace(), true, new StringMemory(path), args[1])
        );
    }

    @Signature({@Arg("protocol"), @Arg("className")})
    public static Memory register(Environment env, Memory... args) {
        String protocol = args[0].toString();
        String className = args[1].toString();

        if (protocol.isEmpty()) {
            exception(env, "Argument 1 (protocol) must be not empty");
            return Memory.FALSE;
        }

        if (!protocol.matches("^[A-Za-z0-9]+$")) {
            exception(env, "Invalid Argument 1 (protocol)");
            return Memory.FALSE;
        }

        ClassEntity classEntity = env.fetchClass(className, true);
        if (classEntity == null){
            exception(env, Messages.ERR_CLASS_NOT_FOUND.fetch(className));
            return Memory.FALSE;
        }

        env.setUserValue(Stream.class.getName() + "#" + protocol, classEntity);
        return Memory.TRUE;
    }

    @Signature({@Arg("protocol")})
    public static Memory unregister(Environment env, Memory... args) {
        String protocol = args[0].toString();

        if (protocol.isEmpty())
            return Memory.FALSE;

        return env.removeUserValue(Stream.class.getName() + "#" + protocol) ? Memory.TRUE : Memory.FALSE;
    }

    public static void initEnvironment(Environment env){
        ClassEntity classEntity = env.fetchClass("php\\io\\FileStream");
        if (classEntity == null)
            throw new CriticalException("php\\io\\FileStream not found");

        env.setUserValue(Stream.class.getName() + "#file", classEntity);
    }

    @Override
    public String getResourceType() {
        return "stream";
    }
}