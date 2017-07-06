package net.kineticraft.lostcity.data.reflect.behavior;

import lombok.Getter;
import net.kineticraft.lostcity.data.JsonData;
import net.kineticraft.lostcity.utils.ReflectionUtil;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A template serializer for storing and loading values in JSON.
 *
 * @param <T>
 * Created by Kneesnap on 7/3/2017.
 */
@Getter
public abstract class DataStore<T> {

    private Class<? extends T> applyTo;
    protected Method saveMethod;

    public DataStore(Class<T> apply) {
        this(apply, "setElement");
    }

    public DataStore(Class<T> apply, String setMethod) {
        this.applyTo = apply;
        this.saveMethod = getMethod(setMethod, String.class, getSaveArgument());
    }

    /**
     * Get the argument to be passed into the setter method.
     * @return method
     */
    protected Class<?> getSaveArgument() {
        return getApplyTo();
    }

    /**
     * Get the value of a field from serialized json.
     * @param data - The data we're reading from.
     * @param key - The key to read from.
     * @param field - The field to set the data to.
     * @return loaded
     */
    public abstract T getField(JsonData data, String key, Field field);

    /**
     * Load a field from json.
     * @param data - The json data we're reading from.
     * @param to - The object we're setting the field of.
     * @param field - The field we're setting the data of.
     */
    public void loadField(JsonData data, Object to, Field field) throws Exception {
        String key = field.getName();
        T value = data.has(key) ? getField(data, key, field) : null;

        if (value != null) // Set the value if there is one.
            field.set(to, value);
    }

    /**
     * Load an object from json data.
     * Does not work with any serializers that use the field parameter in getField yet.
     *
     * @param data
     * @return obj
     */
    public T loadObject(JsonData data) {
        return getField(new JsonData().setElement("temp", data), "temp", null);
    }

    /**
     * Save a field to json.
     * @param data - The json data to put the value into.
     * @param value - The value we're storing in json/
     * @param key - The key to store the value by.
     */
    @SuppressWarnings("unchecked")
    public void saveField(JsonData data, Object value, String key) throws Exception {
        getSaveMethod().invoke(data, key, serialize((T) value));
    }

    /**
     * Perform any special serialization on this object.
     * @param value
     * @return serialized
     */
    public Object serialize(T value) {
        return value;
    }

    protected static Method getMethod(String methodName, Class... argTypes) {
        try {
            return JsonData.class.getDeclaredMethod(methodName, argTypes);
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("Failed to find method " + methodName + ".");
        }
        return null;
    }

    /**
     * Get constructor args from if a field's generic exists.
     * @param f
     * @return args
     */
    protected Object[] getArgs(Field f) {
        Class<?> c = ReflectionUtil.getGenericType(f);
        return c != null ? new Object[] {c} : new Object[0];
    }
}
