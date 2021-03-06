/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Class for handling SFC OVS RPCs
 * <p>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-03-31
 */

package org.opendaylight.sfc.ovs.provider;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.sfc.ovs.api.SfcOvsDataStoreAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.CreateOvsBridgeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.ServiceFunctionForwarderOvsService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.ovs.rev140701.create.ovs.bridge.input.OvsNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class SfcOvsRpc implements ServiceFunctionForwarderOvsService, AutoCloseable {

    private static final String OVSDB_NODE_PREFIX = "ovsdb://";

    /**
     * This method writes a new OVS Bridge into OVSDB Config DataStore. This write event triggers
     * creation of the OVS Bridge in running OpenVSwitch instance identified by OVS Node ip:port
     * locator.
     *
     * <p>
     * @param input RPC input including a OVS Bridge name and parent OVS Node ip:port locator
     * @return RPC output: true if write to OVSDB Config DataStore was successful, otherwise false.
     */
    @Override
    public ListenableFuture<RpcResult<CreateOvsBridgeOutput>> createOvsBridge(CreateOvsBridgeInput input) {
        Preconditions.checkNotNull(input, "create-ovs-bridge RPC input must not be null!");
        Preconditions.checkNotNull(input.getOvsNode(),
                "create-ovs-bridge RPC input container ovs-node must not be null!");
        Preconditions.checkNotNull(input.getName(),
                "create-ovs-bridge RPC input container ovs-name must not be null!");

        RpcResultBuilder<CreateOvsBridgeOutput> rpcResultBuilder;
        NodeId nodeId = null;

        OvsNode ovsNode = input.getOvsNode();

        //create parent OVS Node InstanceIdentifier (based on ip:port locator)
        if (ovsNode.getPort() != null && ovsNode.getIp() != null) {
            nodeId = new NodeId(OVSDB_NODE_PREFIX
                    + ovsNode.getIp().getIpv4Address().getValue()
                    + ":" + ovsNode.getPort().getValue());

        //create parent OVS Node InstanceIdentifier (based on ip)
        } else if (ovsNode.getIp() != null) {
            IpAddress ipAddress = ovsNode.getIp();
            Node node = SfcOvsUtil.getManagerNodeByIp(ipAddress);
            if (node != null) {
                nodeId = node.getNodeId();
            }
        }

        if (nodeId != null) {
            InstanceIdentifier<Node> nodeIID = SfcOvsUtil.buildOvsdbNodeIID(nodeId);

            //build OVS Bridge
            //TODO: separate into function as it will grow in future (including DP locators, etc.)
            OvsdbBridgeAugmentationBuilder ovsdbBridgeBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeBuilder.setBridgeName(new OvsdbBridgeName(input.getName()));
            ovsdbBridgeBuilder.setManagedBy(new OvsdbNodeRef(nodeIID));

            if (SfcOvsDataStoreAPI.putOvsdbBridge(ovsdbBridgeBuilder.build())) {
                rpcResultBuilder = RpcResultBuilder.success(new CreateOvsBridgeOutputBuilder().setResult(true).build());
            } else {
                String message = "Error writing OVS Bridge: '" + input.getName()
                        + "' into OVSDB Configuration DataStore.";
                rpcResultBuilder = RpcResultBuilder.<CreateOvsBridgeOutput>failed()
                        .withError(RpcError.ErrorType.APPLICATION, message);
            }
        } else {
            String message = "Error writing OVS Bridge: '" + input.getName()
                    + "' into OVSDB Configuration DataStore (cannot determine parent NodeId).";
            rpcResultBuilder = RpcResultBuilder.<CreateOvsBridgeOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, message);
        }

        return rpcResultBuilder.buildFuture();
    }

    @Override
    public void close() {
    }
}
