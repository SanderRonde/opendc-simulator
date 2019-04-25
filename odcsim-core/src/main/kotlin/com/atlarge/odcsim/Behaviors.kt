/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@file:JvmName("Behaviors")
package com.atlarge.odcsim

import com.atlarge.odcsim.internal.BehaviorInterpreter
import com.atlarge.odcsim.internal.EmptyBehavior
import com.atlarge.odcsim.internal.IgnoreBehavior

/**
 * This [Behavior] is used to signal that this actor shall terminate voluntarily. If this actor has created child actors
 * then these will be stopped as part of the shutdown procedure.
 */
fun <T : Any> stopped(): Behavior<T> {
    @Suppress("UNCHECKED_CAST")
    return StoppedBehavior as Behavior<T>
}

/**
 * This [Behavior] is used to signal that this actor wants to reuse its previous behavior.
 */
fun <T : Any> same(): Behavior<T> {
    @Suppress("UNCHECKED_CAST")
    return SameBehavior as Behavior<T>
}

/**
 * This [Behavior] is used to signal to the system that the last message or signal went unhandled. This will
 * reuse the previous behavior.
 */
fun <T : Any> unhandled(): Behavior<T> {
    @Suppress("UNCHECKED_CAST")
    return UnhandledBehavior as Behavior<T>
}

/**
 * A factory for [Behavior]. Creation of the behavior instance is deferred until the actor is started.
 */
fun <T : Any> setup(block: (ActorContext<T>) -> Behavior<T>): Behavior<T> {
    return object : DeferredBehavior<T>() {
        override fun invoke(ctx: ActorContext<T>): Behavior<T> = block(ctx)
    }
}

/**
 * A [Behavior] that ignores any incoming message or signal and keeps the same behavior.
 */
fun <T : Any> ignore(): Behavior<T> = IgnoreBehavior.narrow()

/**
 * A [Behavior] that treats every incoming message or signal as unhandled.
 */
fun <T : Any> empty(): Behavior<T> = EmptyBehavior.narrow()

/**
 * Construct a [Behavior] that reacts to incoming messages, provides access to the [ActorContext] and returns the
 * actor's next behavior.
 */
fun <T : Any> receive(handler: (ActorContext<T>, T) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = handler(ctx, msg)
    }
}

/**
 * Construct a [Behavior] that reacts to incoming messages and returns the actor's next behavior.
 */
fun <T : Any> receiveMessage(handler: (T) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = handler(msg)
    }
}

/**
 * Construct a [Behavior] that reacts to incoming signals, provides access to the [ActorContext] and returns the
 * actor's next behavior.
 */
fun <T : Any> receiveSignal(handler: (ActorContext<T>, Signal) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> = handler(ctx, signal)
    }
}

/**
 * Construct a [Behavior] that wraps another behavior instance and uses a [BehaviorInterpreter] to pass incoming
 * messages and signals to the wrapper behavior.
 */
fun <T : Any> wrap(behavior: Behavior<T>, wrap: (BehaviorInterpreter<T>) -> Behavior<T>): Behavior<T> {
    return setup { ctx -> wrap(BehaviorInterpreter(behavior, ctx)) }
}