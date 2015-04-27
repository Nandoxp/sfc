/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ovs.provider.api;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.sfc.sfc_ovs.provider.util.HopOvsdbBridgePair;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarder2;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator2;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.SffDataPlaneLocator2Builder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.bridge.OvsBridge;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.node.OvsNode;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.options.OvsOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.sff.data.plane.locator.DataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Other;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has the APIs to map SFC Service Function Forwarder to OVS Bridge
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @see SfcSffToOvsMappingAPI
 * <p/>
 * <p/>
 * <p/>
 * @since 2015-03-23
 */
public class SfcSffToOvsMappingAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcSffToOvsMappingAPI.class);

    private static final String VXLAN = "-vxlan-";
    private static final String TO = "to";

    /**
     * Builds OvsdbBridgeAugmentation from ServiceFunctionForwarder object. Built augmentation is intended to be written
     * into Configuration Ovsdb DataStore (network-topology/topology/ovsdb:1).
     * <p/>
     *
     * @param serviceFunctionForwarder ServiceFunctionForwarder Object
     * @return OvsdbBridgeAugmentation Object
     */
    public static OvsdbBridgeAugmentation buildOvsdbBridgeAugmentation(ServiceFunctionForwarder serviceFunctionForwarder) {
        Preconditions.checkNotNull(serviceFunctionForwarder);

        OvsdbBridgeAugmentationBuilder ovsdbBridgeBuilder = new OvsdbBridgeAugmentationBuilder();

        ServiceFunctionForwarder2 serviceForwarderOvsBridgeAugmentation =
                serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder2.class);
        if (serviceForwarderOvsBridgeAugmentation != null) {
            OvsBridge serviceForwarderOvsBridge = serviceForwarderOvsBridgeAugmentation.getOvsBridge();

            if (serviceForwarderOvsBridge != null) {
                ovsdbBridgeBuilder.setBridgeName(new OvsdbBridgeName(serviceForwarderOvsBridge.getBridgeName()));
                ovsdbBridgeBuilder.setBridgeUuid(serviceForwarderOvsBridge.getUuid());
            } else {
                LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Bridge augmentation on SFF {}", serviceFunctionForwarder.getName());
                return null;
            }

        } else {
            LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Bridge augmentation on SFF {}", serviceFunctionForwarder.getName());
            return null;
        }

        ServiceFunctionForwarder1 serviceForwarderOvsNodeAugmentation =
                serviceFunctionForwarder.getAugmentation(ServiceFunctionForwarder1.class);
        if (serviceForwarderOvsNodeAugmentation != null) {
            OvsNode serviceForwarderOvsNode = serviceForwarderOvsNodeAugmentation.getOvsNode();

            if (serviceForwarderOvsNode != null) {
                ovsdbBridgeBuilder.setManagedBy(serviceForwarderOvsNode.getNodeId());
            } else {
                LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Node augmentation on SFF {}", serviceFunctionForwarder.getName());
                return null;
            }

        } else {
            LOG.info("Cannot build OvsdbBridgeAugmentation. Missing OVS Node augmentation on SFF {}", serviceFunctionForwarder.getName());
            return null;
        }

        return ovsdbBridgeBuilder.build();
    }

    /**
     * Builds List<OvsdbTerminationPointAugmentation> from List<SffDataPlanLocator>.
     * In other words, it transforms ServiceForwarder DataPlane locators to OVS TerminationPoints.
     * <p/>
     * Built list of augmentations is intended to be written into Configuration Ovsdb DataStore
     * (network-topology/topology/ovsdb:1).
     * <p/>
     *
     * @param sffDataPlaneLocatorList List<SffDataPlaneLocator>
     * @return List<OvsdbTerminationPointAugmentation>
     */
    public static List<OvsdbTerminationPointAugmentation> buildTerminationPointAugmentationList(
            List<SffDataPlaneLocator> sffDataPlaneLocatorList) {

        Preconditions.checkNotNull(sffDataPlaneLocatorList,
                "Cannot build TerminationPointAugmentation, SffDataPlaneLocatorList is null.");

        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList = new ArrayList<>();

        for (SffDataPlaneLocator sffDataPlaneLocator : sffDataPlaneLocatorList) {
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = new OvsdbTerminationPointAugmentationBuilder();

            ovsdbTerminationPointBuilder.setName(sffDataPlaneLocator.getName());
            ovsdbTerminationPointBuilder.setInterfaceType(getDataPlaneLocatorInterfaceType(sffDataPlaneLocator.getDataPlaneLocator()));
            ovsdbTerminationPointBuilder.setOptions(getSffDataPlaneLocatorOptions(sffDataPlaneLocator));
            ovsdbTerminationPointList.add(ovsdbTerminationPointBuilder.build());
        }

        return ovsdbTerminationPointList;
    }

    private static List<Options> getDataPlaneLocatorOptions(DataPlaneLocator dataPlaneLocator) {
        Preconditions.checkNotNull(dataPlaneLocator, "Cannot determine DataPlaneLocator locator type, dataPlaneLocator is null.");
        List<Options> options = new ArrayList<>();

        if (dataPlaneLocator.getLocatorType() != null) {
            Class<? extends DataContainer> locatorType = dataPlaneLocator.getLocatorType().getImplementedInterface();
            if (locatorType.isAssignableFrom(Ip.class)) {
                Ip ipPortLocator = (Ip) dataPlaneLocator.getLocatorType();

                OptionsBuilder optionsIpBuilder = new OptionsBuilder();
                optionsIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
                optionsIpBuilder.setValue(ipPortLocator.getIp().getIpv4Address().getValue());

                options.add(optionsIpBuilder.build());
            }
        } else {
            LOG.warn("Cannot determine DataPlaneLocator locator type, dataPlaneLocator.getLocatorType() is null.");
        }

        return options;
    }

    private static List<Options> getSffDataPlaneLocatorOptions(SffDataPlaneLocator sffDataPlaneLocator) {
        Preconditions.checkNotNull(sffDataPlaneLocator, "Cannot gather SffDataPlaneLocator Options, sffDataPlaneLocator is null.");
        List<Options> options = new ArrayList<>();

        SffDataPlaneLocator2 sffDataPlaneLocatorOvsOptions = sffDataPlaneLocator.getAugmentation(SffDataPlaneLocator2.class);
        OvsOptions ovsOptions = sffDataPlaneLocatorOvsOptions.getOvsOptions();

        if (ovsOptions.getLocalIp() != null) {
            OptionsBuilder optionsLocalIpBuilder = new OptionsBuilder();
            optionsLocalIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
            optionsLocalIpBuilder.setValue(SfcOvsUtil.convertIpAddressToString(ovsOptions.getLocalIp()));
            options.add(optionsLocalIpBuilder.build());
        } else {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_LOCAL_IP);
        }

        if (ovsOptions.getRemoteIp() != null) {
            OptionsBuilder optionsDstIpBuilder = new OptionsBuilder();
            optionsDstIpBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_REMOTE_IP);
            optionsDstIpBuilder.setValue(SfcOvsUtil.convertIpAddressToString(ovsOptions.getRemoteIp()));
            options.add(optionsDstIpBuilder.build());
        } else {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_REMOTE_IP);
        }

        if (ovsOptions.getDstPort() != null) {
            OptionsBuilder optionsDstPortBuilder = new OptionsBuilder();
            optionsDstPortBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_DST_PORT);
            optionsDstPortBuilder.setValue(ovsOptions.getDstPort().getValue().toString());
            options.add(optionsDstPortBuilder.build());
        } else {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_DST_PORT);
        }

        if (ovsOptions.getKey() != null) {
            OptionsBuilder optionsKeyBuilder = new OptionsBuilder();
            optionsKeyBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_KEY);
            optionsKeyBuilder.setValue(ovsOptions.getKey());
            options.add(optionsKeyBuilder.build());
        } else {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_KEY);
        }

        if (ovsOptions.getNsp() != null) {
            OptionsBuilder optionsNspBuilder = new OptionsBuilder();
            optionsNspBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSP);
            optionsNspBuilder.setValue(ovsOptions.getNsp());
            options.add(optionsNspBuilder.build());
        } else {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSP);
        }

        if (ovsOptions.getNsi() != null) {
            OptionsBuilder optionsNsiBuilder = new OptionsBuilder();
            optionsNsiBuilder.setOption(SfcOvsUtil.OVSDB_OPTION_NSI);
            optionsNsiBuilder.setValue(ovsOptions.getNsi());
            options.add(optionsNsiBuilder.build());
        } else {
            LOG.debug("Option: {} is null.", SfcOvsUtil.OVSDB_OPTION_NSI);
        }

        return options;
    }

    private static Class<? extends InterfaceTypeBase> getDataPlaneLocatorInterfaceType(DataPlaneLocator dataPlaneLocator) {
        Preconditions.checkNotNull(dataPlaneLocator, "Cannot determine DataPlaneLocator transport type, dataPlaneLocator is null.");

        if (dataPlaneLocator.getTransport() == Other.class) {
            return InterfaceTypeInternal.class;
        } else if (dataPlaneLocator.getTransport() == VxlanGpe.class) {
            return InterfaceTypeVxlan.class;
        } else {
            LOG.warn("Cannot determine DataPlaneLocator transport type, dataPlaneLocator.getTransport() is null.");

            //TODO: remove once MDSAL OVSDB will not require interface type to be specified
            LOG.warn("Falling back to InterfaceTypeInternal");
            return InterfaceTypeInternal.class;
        }
    }

    public static SffDataPlaneLocator buildVxlanTunnelDataPlaneLocator(RenderedServicePath renderedServicePath,
                                                                       HopOvsdbBridgePair hopOvsdbBridgePairFrom,
                                                                       HopOvsdbBridgePair hopOvsdbBridgePairTo) {
        Preconditions.checkNotNull(renderedServicePath, "Cannot build VxlanTunnel DataPlane locator, renderedServicePath is null");
        Preconditions.checkNotNull(hopOvsdbBridgePairFrom, "Cannot build VxlanTunnel DataPlane locator, source Hop is null");
        Preconditions.checkNotNull(hopOvsdbBridgePairTo, "Cannot build VxlanTunnel DataPlane locator, destination Hop is null");

        SffDataPlaneLocatorBuilder sffDataPlaneLocatorBuilder = new SffDataPlaneLocatorBuilder();
        //the name will be e.g. RSP1-vxlan-0to1
        sffDataPlaneLocatorBuilder
                .setName(buildVxlanTunnelDataPlaneLocatorName(renderedServicePath, hopOvsdbBridgePairFrom, hopOvsdbBridgePairTo));

        //build IP:Port locator
        IpAddress ipAddress = SfcOvsToSffMappingAPI.getOvsBridgeLocalIp(hopOvsdbBridgePairFrom.ovsdbBridgeAugmentation);
        IpBuilder ipBuilder = new IpBuilder();
        ipBuilder.setIp(ipAddress);
        ipBuilder.setPort(SfcOvsUtil.NSH_VXLAN_TUNNEL_PORT);

        //build Vxlan DataPlane locator
        DataPlaneLocatorBuilder dataPlaneLocatorBuilder = new DataPlaneLocatorBuilder();
        dataPlaneLocatorBuilder.setTransport(VxlanGpe.class);
        dataPlaneLocatorBuilder.setLocatorType(ipBuilder.build());
        sffDataPlaneLocatorBuilder.setDataPlaneLocator(dataPlaneLocatorBuilder.build());

        //build OVS Options for Vxlan tunnel
        OvsOptionsBuilder ovsOptionsBuilder = new OvsOptionsBuilder();
        ovsOptionsBuilder.setLocalIp(ipAddress);
        ovsOptionsBuilder.setRemoteIp(SfcOvsToSffMappingAPI.getOvsBridgeLocalIp(hopOvsdbBridgePairTo.ovsdbBridgeAugmentation));
        ovsOptionsBuilder.setDstPort(SfcOvsUtil.NSH_VXLAN_TUNNEL_PORT);
        ovsOptionsBuilder.setNsp(renderedServicePath.getPathId().toString());
        ovsOptionsBuilder.setNsi(hopOvsdbBridgePairFrom.renderedServicePathHop.getServiceIndex().toString());
        ovsOptionsBuilder.setKey(renderedServicePath.getPathId().toString());

        //add OVS Options augmentation to SffDataPlaneLocator
        SffDataPlaneLocator2Builder sffDataPlaneLocator2Builder = new SffDataPlaneLocator2Builder();
        sffDataPlaneLocator2Builder.setOvsOptions(ovsOptionsBuilder.build());
        sffDataPlaneLocatorBuilder.addAugmentation(SffDataPlaneLocator2.class, sffDataPlaneLocator2Builder.build());

        return sffDataPlaneLocatorBuilder.build();
    }

    public static String buildVxlanTunnelDataPlaneLocatorName(RenderedServicePath renderedServicePath,
                                                              HopOvsdbBridgePair hopOvsdbBridgePairFrom,
                                                              HopOvsdbBridgePair hopOvsdbBridgePairTo) {
        Preconditions.checkNotNull(renderedServicePath,
                "Cannot build VxlanTunnel DataPlane locator Name, renderedServicePath is null");
        Preconditions.checkNotNull(hopOvsdbBridgePairFrom,
                "Cannot build VxlanTunnel DataPlane locator Name, source Hop is null");
        Preconditions.checkNotNull(hopOvsdbBridgePairTo,
                "Cannot build VxlanTunnel DataPlane locator Name, destination Hop is null");

        //the name will be e.g. RSP1-vxlan-0to1
        return (renderedServicePath.getName() + VXLAN
                + hopOvsdbBridgePairFrom.renderedServicePathHop.getHopNumber() + TO
                + hopOvsdbBridgePairTo.renderedServicePathHop.getHopNumber());
    }
}
