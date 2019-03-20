package com.github.uuidcode.jmx.test;

import java.util.Hashtable;

import javax.management.ObjectName;

public class JMXMeta {
    private String name;
    private String key;
    private String type;

    public static JMXMeta of() {
        return new JMXMeta();
    }

    public static JMXMeta of(ObjectName objectName) {
        Hashtable<String, String> keyPropertyList =
            objectName.getKeyPropertyList();

        return of()
            .setName(objectName.getCanonicalName())
            .setKey(keyPropertyList.get("name"))
            .setType(keyPropertyList.get("type"));
    }

    public String getType() {
        return this.type;
    }

    public JMXMeta setType(String type) {
        this.type = type;
        return this;
    }
    public String getKey() {
        return this.key;
    }

    public JMXMeta setKey(String key) {
        this.key = key;
        return this;
    }
    public String getName() {
        return this.name;
    }

    public JMXMeta setName(String name) {
        this.name = name;
        return this;
    }
}
