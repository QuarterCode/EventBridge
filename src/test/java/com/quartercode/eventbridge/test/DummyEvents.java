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

package com.quartercode.eventbridge.test;

import com.quartercode.eventbridge.bridge.Event;

public class DummyEvents {

    @SuppressWarnings ("serial")
    public static class EmptyEvent1 implements Event {

    }

    @SuppressWarnings ("serial")
    public static class EmptyEvent2 implements Event {

    }

    @SuppressWarnings ("serial")
    public static class EmptyEvent3 implements Event {

    }

    @SuppressWarnings ("serial")
    public static class EmptyEvent4 implements Event {

    }

    @SuppressWarnings ("serial")
    public static class EmptyEvent5 implements Event {

    }

    @SuppressWarnings ("serial")
    public static class InheritingEventSuper implements Event {

    }

    public static interface InheritingEventInterface extends Event {

    }

    @SuppressWarnings ("serial")
    public static class InheritingEvent extends InheritingEventSuper implements InheritingEventInterface {

    }

    @SuppressWarnings ("serial")
    public static class CallableEvent implements Event {

        private boolean called;

        public void call() {

            called = true;
        }

        public boolean isCalled() {

            return called;
        }

    }

    private DummyEvents() {

    }

}
