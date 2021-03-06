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

package com.quartercode.eventbridge.test.def.extra.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.quartercode.eventbridge.bridge.Bridge;
import com.quartercode.eventbridge.bridge.BridgeConnector;
import com.quartercode.eventbridge.bridge.BridgeConnectorException;
import com.quartercode.eventbridge.bridge.Event;
import com.quartercode.eventbridge.bridge.EventPredicate;
import com.quartercode.eventbridge.bridge.module.ConnectorSenderModule;
import com.quartercode.eventbridge.bridge.module.ConnectorSenderModule.SpecificConnectorSendInterceptor;
import com.quartercode.eventbridge.bridge.module.EventHandler;
import com.quartercode.eventbridge.bridge.module.LowLevelHandler;
import com.quartercode.eventbridge.bridge.module.LowLevelHandlerModule;
import com.quartercode.eventbridge.channel.ChannelInvocation;
import com.quartercode.eventbridge.def.bridge.DefaultBridge;
import com.quartercode.eventbridge.def.extra.extension.DefaultSendPredicateCheckExtension;
import com.quartercode.eventbridge.extra.connector.LocalBridgeConnector;
import com.quartercode.eventbridge.extra.predicate.TypePredicate;
import com.quartercode.eventbridge.test.DummyEvents.EmptyEvent1;
import com.quartercode.eventbridge.test.DummyEvents.EmptyEvent2;
import com.quartercode.eventbridge.test.DummyEvents.EmptyEvent3;
import com.quartercode.eventbridge.test.DummyEvents.EmptyEvent4;
import com.quartercode.eventbridge.test.DummyEvents.EmptyEvent5;
import com.quartercode.eventbridge.test.DummyInterceptors.DummySpecificConnectorSendInterceptor;

@SuppressWarnings ("unchecked")
public class DefaultSendPredicateCheckExtensionTest {

    private static final Class<Event> INTERNAL_EVENT_TYPE;

    static {

        try {
            INTERNAL_EVENT_TYPE = (Class<Event>) Class.forName(DefaultSendPredicateCheckExtension.class.getName() + "$SetPredicatesEvent");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private static Pair<BridgeConnector, Class<?>[]> pair(BridgeConnector connector, Class<?>... events) {

        return Pair.of(connector, events);
    }

    @Rule
    public JUnitRuleMockery                    context = new JUnitRuleMockery();

    private Bridge                             bridge1;
    private Bridge                             bridge2;
    private Bridge                             bridge3;
    private BridgeConnector                    bridge1To2Connector;
    private BridgeConnector                    bridge1To3Connector;
    @Mock
    private SpecificConnectorSendInterceptor   interceptor;

    private DefaultSendPredicateCheckExtension bridge1Extension;
    private DefaultSendPredicateCheckExtension bridge2Extension;
    private DefaultSendPredicateCheckExtension bridge3Extension;

    @Before
    public void setUp() {

        // Don't mock because it would be too much work
        bridge1 = new DefaultBridge();
        bridge2 = new DefaultBridge();
        bridge3 = new DefaultBridge();
        bridge1To2Connector = new LocalBridgeConnector(bridge2);
        bridge1To3Connector = new LocalBridgeConnector(bridge3);

        // Add the mocked connector send interceptor to bridge 1
        bridge1.getModule(ConnectorSenderModule.class).getSpecificChannel().addInterceptor(new DummySpecificConnectorSendInterceptor(interceptor), 1);

        // Allow the internal events to flow through the interceptor
        // @formatter:off
        context.checking(new Expectations() {{

            allowing(interceptor).send(with(any(ChannelInvocation.class)), with(any(INTERNAL_EVENT_TYPE)), with(any(BridgeConnector.class)));

        }});
        // @formatter:on

        // Install the send predicate check extensions on both bridges
        bridge1Extension = new DefaultSendPredicateCheckExtension();
        bridge2Extension = new DefaultSendPredicateCheckExtension();
        bridge3Extension = new DefaultSendPredicateCheckExtension();
        bridge1.addModule(bridge1Extension);
        bridge2.addModule(bridge2Extension);
        bridge3.addModule(bridge3Extension);
    }

    private Event[] beforeCustomActions(Pair<BridgeConnector, Class<?>[]>... expectedEvents) {

        Event[] events = { new EmptyEvent1(), new EmptyEvent2(), new EmptyEvent3(), new EmptyEvent4(), new EmptyEvent5() };

        final List<Pair<BridgeConnector, Event>> actualExpectedEvents = new ArrayList<>();
        for (Event event : events) {
            for (Pair<BridgeConnector, Class<?>[]> expectation : expectedEvents) {
                if (Arrays.asList(expectation.getRight()).contains(event.getClass())) {
                    actualExpectedEvents.add(Pair.of(expectation.getLeft(), event));
                }
            }
        }

        // @formatter:off
        context.checking(new Expectations() {{

            for (Pair<BridgeConnector, Event> expectedEvent : actualExpectedEvents) {
                oneOf(interceptor).send(with(any(ChannelInvocation.class)), with(expectedEvent.getRight()), with(expectedEvent.getLeft()));
            }

        }});
        // @formatter:on

        return events;
    }

    private void afterCustomActions(Event[] data) {

        for (Event event : data) {
            bridge1.send(event);
        }
    }

    private void connect() throws BridgeConnectorException {

        bridge1.addConnector(bridge1To2Connector);
        bridge1.addConnector(bridge1To3Connector);
    }

    private void removeExtensions() {

        bridge1.removeModule(bridge1Extension);
        bridge1Extension = null;
        bridge2.removeModule(bridge2Extension);
        bridge2Extension = null;
        bridge3.removeModule(bridge3Extension);
        bridge3Extension = null;
    }

    @Test
    public void testRemoveBeforeConnect() throws BridgeConnectorException {

        // Expect all events to be sent
        Class<?>[] allEvents = { EmptyEvent1.class, EmptyEvent2.class, EmptyEvent3.class, EmptyEvent4.class, EmptyEvent5.class };
        Event[] data = beforeCustomActions(pair(bridge1To2Connector, allEvents), pair(bridge1To3Connector, allEvents));

        removeExtensions();
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testRemoveAfterConnect() throws BridgeConnectorException {

        // Expect all events to be sent
        Class<?>[] allEvents = { EmptyEvent1.class, EmptyEvent2.class, EmptyEvent3.class, EmptyEvent4.class, EmptyEvent5.class };
        Event[] data = beforeCustomActions(pair(bridge1To2Connector, allEvents), pair(bridge1To3Connector, allEvents));

        connect();
        removeExtensions();

        afterCustomActions(data);
    }

    @Test
    public void testSuppressLocalHandlingOfInternalEvents() throws BridgeConnectorException {

        final EventHandler<Event> handler = context.mock(EventHandler.class);

        // @formatter:off
        context.checking(new Expectations() {{

            never(handler).handle(with(any(Event.class)));

        }});
        // @formatter:on

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
    }

    // ----- One Bridge Filtering Tests -----

    @Test
    public void testOneBridgeAddHandlerBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testOneBridgeAddHandlerAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class));

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));

        afterCustomActions(data);
    }

    @Test
    public void testOneBridgeAddMultipleHandlersBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testOneBridgeAddMultipleHandlersAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class));

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));

        afterCustomActions(data);
    }

    @Test
    public void testOneBridgeRemoveHandlerBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        EmptyLowLevelHandler removeHandler = new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(removeHandler);
        bridge2.getModule(LowLevelHandlerModule.class).removeHandler(removeHandler);

        connect();

        afterCustomActions(data);
    }

    @Test
    public void testOneBridgeRemoveHandlerAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class));

        connect();

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        EmptyLowLevelHandler removeHandler = new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(removeHandler);
        bridge2.getModule(LowLevelHandlerModule.class).removeHandler(removeHandler);

        afterCustomActions(data);
    }

    // ----- Two Bridges Filtering Tests -----

    @Test
    public void testTwoBridgesAddHandlerSameEventsBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class), pair(bridge1To3Connector, EmptyEvent1.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddHandlerSameEventsAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class), pair(bridge1To3Connector, EmptyEvent1.class));

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddHandlerOtherEventsBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class), pair(bridge1To3Connector, EmptyEvent2.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddHandlerOtherEventsAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class), pair(bridge1To3Connector, EmptyEvent2.class));

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddMultipleHandlersSameEventsBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class), pair(bridge1To3Connector, EmptyEvent1.class, EmptyEvent2.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddMultipleHandlersSameEventsAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class), pair(bridge1To3Connector, EmptyEvent1.class, EmptyEvent2.class));

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddMultipleHandlersOtherEventsBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class), pair(bridge1To3Connector, EmptyEvent3.class, EmptyEvent4.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent3.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent4.class)));
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddMultipleHandlersOtherEventsAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class), pair(bridge1To3Connector, EmptyEvent3.class, EmptyEvent4.class));

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent3.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent4.class)));

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddMultipleHandlersPartlySameEventsBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class), pair(bridge1To3Connector, EmptyEvent1.class, EmptyEvent3.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent3.class)));
        connect();

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesAddMultipleHandlersPartlySameEventsAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class, EmptyEvent2.class), pair(bridge1To3Connector, EmptyEvent1.class, EmptyEvent3.class));

        connect();
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent3.class)));

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesRemoveHandlerBeforeConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class), pair(bridge1To3Connector, EmptyEvent2.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        EmptyLowLevelHandler removeHandler1 = new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(removeHandler1);
        bridge2.getModule(LowLevelHandlerModule.class).removeHandler(removeHandler1);

        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        EmptyLowLevelHandler removeHandler2 = new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(removeHandler2);
        bridge3.getModule(LowLevelHandlerModule.class).removeHandler(removeHandler2);

        connect();

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesRemoveHandlerAfterConnect() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class), pair(bridge1To3Connector, EmptyEvent2.class));

        connect();

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        EmptyLowLevelHandler removeHandler1 = new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class));
        bridge2.getModule(LowLevelHandlerModule.class).addHandler(removeHandler1);
        bridge2.getModule(LowLevelHandlerModule.class).removeHandler(removeHandler1);

        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent2.class)));
        EmptyLowLevelHandler removeHandler2 = new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(removeHandler2);
        bridge3.getModule(LowLevelHandlerModule.class).removeHandler(removeHandler2);

        afterCustomActions(data);
    }

    @Test
    public void testTwoBridgesRemoveConnection() throws BridgeConnectorException {

        Event[] data = beforeCustomActions(pair(bridge1To2Connector, EmptyEvent1.class));

        bridge2.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        bridge3.getModule(LowLevelHandlerModule.class).addHandler(new EmptyLowLevelHandler(new TypePredicate<>(EmptyEvent1.class)));
        connect();

        bridge1.removeConnector(bridge1To3Connector);

        afterCustomActions(data);
    }

    private class EmptyLowLevelHandler implements LowLevelHandler {

        private final EventPredicate<?> predicate;

        private EmptyLowLevelHandler(EventPredicate<?> predicate) {

            this.predicate = predicate;
        }

        @Override
        public EventPredicate<?> getPredicate() {

            return predicate;
        }

        @Override
        public void handle(Event event, BridgeConnector source) {

        }

    }

}
