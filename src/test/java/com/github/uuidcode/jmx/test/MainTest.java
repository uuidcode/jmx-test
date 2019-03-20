package com.github.uuidcode.jmx.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import static com.github.uuidcode.util.CoreUtil.unchecked;
import static java.util.stream.Collectors.toMap;
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
        MBeanServerConnection connection = getConnection();
        ObjectName memory = new ObjectName("java.lang:type=Memory");
        CompositeData compositeData = (CompositeData) connection.getAttribute(memory, "HeapMemoryUsage");
        Long used = (Long) compositeData.get("used");

        if (logger.isDebugEnabled()) {
            logger.debug(">>> local used: {}", CoreUtil.toJson(used));
        }
    }

    private MBeanServerConnection getConnection() throws Exception {
        VirtualMachine virtualMachine = this.getVirtualMachine();
        JMXServiceURL url = this.getJMXServiceURL(virtualMachine);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(url);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
        this.printMBeanInfo(connection);

        return connection;
    }

    private void printMBeanInfo(MBeanServerConnection connection) throws Exception {
        Set<ObjectName> objectNameSet = connection.queryNames(null, null);
        Map<String, MBeanInfo> mBeanInfoMap = objectNameSet.stream()
            .collect(toMap(ObjectName::getCanonicalName, unchecked(connection::getMBeanInfo)));

        if (logger.isDebugEnabled()) {
            logger.debug(">>> local mBeanInfoMap: {}", CoreUtil.toJson(mBeanInfoMap));
        }
    }

    private JMXServiceURL getJMXServiceURL(VirtualMachine virtualMachine) throws Exception {
        String agent = virtualMachine.getSystemProperties().getProperty("java.home") +
            File.separator + "lib" + File.separator + "management-agent.jar";
        virtualMachine.loadAgent(agent);

        String localConnectorAddress = virtualMachine.getAgentProperties()
            .getProperty("com.sun.management.jmxremote.localConnectorAddress");

        return new JMXServiceURL(localConnectorAddress);
    }

    private VirtualMachine getVirtualMachine() throws Exception {
        AttachProvider attachProvider = AttachProvider.providers().get(0);

        List<VirtualMachineDescriptor> virtualMachineDescriptorList = attachProvider.listVirtualMachines();

        if (logger.isDebugEnabled()) {
            logger.debug(">>> local virtualMachineDescriptorList: {}", CoreUtil.toJson(virtualMachineDescriptorList));
        }

        VirtualMachineDescriptor virtualMachineDescriptor = virtualMachineDescriptorList.stream()
            .filter(CoreUtil.equals(VirtualMachineDescriptor::displayName, Main.class.getName()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("not exist"));


        return attachProvider.attachVirtualMachine(virtualMachineDescriptor);
    }
}