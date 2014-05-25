/*
 * This file is part of EventBridge.
 * Copyright (c) 2014 QuarterCode <http://www.quartercode.com/>
 *
 * EventBridge is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * EventBridge is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EventBridge. If not, see <http://www.gnu.org/licenses/>.
 */

package com.quartercode.eventbridge.test.def.bridge;

import java.util.Arrays;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.quartercode.eventbridge.bridge.Bridge;
import com.quartercode.eventbridge.bridge.BridgeConnector;
import com.quartercode.eventbridge.bridge.BridgeConnectorException;
import com.quartercode.eventbridge.bridge.HandlerModule;
import com.quartercode.eventbridge.bridge.SenderModule;
import com.quartercode.eventbridge.bridge.SenderModule.ConnectorSendInterceptor;
import com.quartercode.eventbridge.bridge.SenderModule.GlobalSendInterceptor;
import com.quartercode.eventbridge.channel.ChannelInvocation;
import com.quartercode.eventbridge.def.bridge.DefaultSenderModule;
import com.quartercode.eventbridge.test.def.bridge.DummyEvents.EmptyEvent1;
import com.quartercode.eventbridge.test.def.bridge.DummyInterceptors.DummyConnectorSendInterceptor;
import com.quartercode.eventbridge.test.def.bridge.DummyInterceptors.DummyGlobalSendInterceptor;

public class DefaultSenderModuleTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Mock
    private Bridge          bridge;
    private SenderModule    senderModule;

    @Before
    public void setUp() {

        senderModule = new DefaultSenderModule(bridge);
    }

    @SuppressWarnings ("unchecked")
    @Test
    public void testSend() throws BridgeConnectorException {

        final EmptyEvent1 event = new EmptyEvent1();

        final GlobalSendInterceptor globalInterceptor = context.mock(GlobalSendInterceptor.class);
        senderModule.getGlobalSendChannel().addInterceptor(new DummyGlobalSendInterceptor(globalInterceptor), 10);

        final ConnectorSendInterceptor connectorInterceptor = context.mock(ConnectorSendInterceptor.class);
        senderModule.getConnectorSendChannel().addInterceptor(new DummyConnectorSendInterceptor(connectorInterceptor), 10);

        final BridgeConnector connector = context.mock(BridgeConnector.class);
        final HandlerModule handlerModule = context.mock(HandlerModule.class);

        // @formatter:off
        context.checking(new Expectations() {{

            allowing(bridge).getConnectors();
                will(returnValue(Arrays.asList(connector)));
            allowing(bridge).getHandlerModule();
                will(returnValue(handlerModule));

            final Sequence sendChain = context.sequence("sendChain");
            oneOf(globalInterceptor).send(with(any(ChannelInvocation.class)), with(event)); inSequence(sendChain);
            oneOf(connectorInterceptor).send(with(any(ChannelInvocation.class)), with(connector), with(event)); inSequence(sendChain);
            oneOf(connector).send(event); inSequence(sendChain);
            oneOf(handlerModule).handle(event); inSequence(sendChain);

        }});
        // @formatter:on

        senderModule.send(event);
    }

    @Test
    public void testSendWithException() throws BridgeConnectorException {

        final EmptyEvent1 event = new EmptyEvent1();

        final BridgeConnector connector = context.mock(BridgeConnector.class);
        final HandlerModule handlerModule = context.mock(HandlerModule.class);

        // @formatter:off
        context.checking(new Expectations() {{

            allowing(bridge).getConnectors();
                will(returnValue(Arrays.asList(connector)));
            allowing(bridge).getHandlerModule();
                will(returnValue(handlerModule));

            oneOf(connector).send(event);
                will(throwException(new BridgeConnectorException(connector)));
            allowing(handlerModule).handle(event);

        }});
        // @formatter:on

        // Assert that the exception is suppressed by the sender module
        senderModule.send(event);
    }

}