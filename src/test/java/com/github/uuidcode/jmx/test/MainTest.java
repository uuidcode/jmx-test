package com.github.uuidcode.jmx.test;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Test;
import org.slf4j.Logger;

import com.github.uuidcode.util.CoreUtil;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;

import static java.util.Optional.ofNullable;
import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;

public class MainTest {
    protected static Logger logger = getLogger(MainTest.class);

    @Test
    public void remote() throws Exception{
        JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        Set<ObjectName> objectNameSet = mBeanServerConnection.queryNames(null, null);

        for (ObjectName objectName : objectNameSet) {
            MBeanInfo mBeanInfo = mBeanServerConnection.getMBeanInfo(objectName);

            if (logger.isDebugEnabled()) {
                logger.debug(">>> test mBeanInfo: {}", CoreUtil.toJson(mBeanInfo));
            }
        }
    }

    @Test
    public void listVirtualMachines() {
        AttachProvider attachProvider = AttachProvider.providers().get(0);
        List<VirtualMachineDescriptor> virtualMachineDescriptorList = attachProvider.listVirtualMachines();

        if (logger.isDebugEnabled()) {
            logger.debug(">>> local virtualMachineDescriptorList: {}", CoreUtil.toJson(virtualMachineDescriptorList));
        }
    }

    @Test
    public void local() throws Exception {
        AttachProvider attachProvider = AttachProvider.providers().get(0);

        List<VirtualMachineDescriptor> virtualMachineDescriptorList = attachProvider.listVirtualMachines();

        if (logger.isDebugEnabled()) {
            logger.debug(">>> local virtualMachineDescriptorList: {}", CoreUtil.toJson(virtualMachineDescriptorList));
        }

        VirtualMachineDescriptor virtualMachineDescriptor = virtualMachineDescriptorList.stream()
            .filter(CoreUtil.equals(VirtualMachineDescriptor::displayName, Main.class.getName()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("not exist"));


        VirtualMachine virtualMachine = attachProvider.attachVirtualMachine(virtualMachineDescriptor);
        String agent = virtualMachine.getSystemProperties().getProperty("java.home") +
            File.separator + "lib" + File.separator + "management-agent.jar";
        virtualMachine.loadAgent(agent);

        String localConnectorAddress = virtualMachine.getAgentProperties()
            .getProperty("com.sun.management.jmxremote.localConnectorAddress");

        JMXServiceURL url = new JMXServiceURL(localConnectorAddress);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(url);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

        Set<ObjectName> objectNameSet = connection.queryNames(null, null);

        List<JMXMeta> jmxMetaList = objectNameSet.stream()
            .map(JMXMeta::of)
            .collect(Collectors.toList());

        if (logger.isDebugEnabled()) {
            logger.debug(">>> local jmxMetaList: {}", CoreUtil.toJson(jmxMetaList));
        }

        ObjectName memory = new ObjectName("java.lang:type=Memory");
        CompositeData cd = (CompositeData) connection.getAttribute(memory, "HeapMemoryUsage");
        Long used = (Long) cd.get("used");

        if (logger.isDebugEnabled()) {
            logger.debug(">>> local used: {}", CoreUtil.toJson(used));
        }
    }
}