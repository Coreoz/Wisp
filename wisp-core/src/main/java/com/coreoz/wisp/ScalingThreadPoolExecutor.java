package com.coreoz.wisp;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

/**
 * A thread pool executor that will reuse newly created threads
 * instead of building them when {@code maxPoolSize} is reached.<br>
 * <br>
 * Credits to Mohammad Nadeem.
 * @see <a href="https://reachmnadeem.wordpress.com/2017/01/15/scalable-java-tpe/">
 * https://reachmnadeem.wordpress.com/2017/01/15/scalable-java-tpe/
 * </a>
 */
final class ScalingThreadPoolExecutor extends ThreadPoolExecutor {

	ScalingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
			TimeUnit keepAliveUnit, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
				new DynamicBlockingQueue<>(new LinkedTransferQueue<Runnable>()), threadFactory, new ForceQueuePolicy());
	}

	@Override
	public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
		throw new IllegalArgumentException("Cant set rejection handler");
	}

	private static class ForceQueuePolicy implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			try {
				// Rejected work add to Queue.
				executor.getQueue().put(r);
			} catch (InterruptedException e) {
				// should never happen since we never wait
				throw new RejectedExecutionException(e);
			}
		}
	}

	private static class DynamicBlockingQueue<E> implements TransferQueue<E> {
		private final TransferQueue<E> delegate;

		public DynamicBlockingQueue(final TransferQueue<E> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean offer(E o) {
			return tryTransfer(o);
		}

		@Override
		public boolean add(E o) {
			if (this.delegate.add(o)) {
				return true;
			} else {// Not possible in our case
				throw new IllegalStateException("Queue full");
			}
		}

		@Override
		public E remove() {
			return this.delegate.remove();
		}

		@Override
		public E poll() {
			return this.delegate.poll();
		}

		@Override
		public E element() {
			return this.delegate.element();
		}

		@Override
		public E peek() {
			return this.delegate.peek();
		}

		@Override
		public int size() {
			return this.delegate.size();
		}

		@Override
		public boolean isEmpty() {
			return this.delegate.isEmpty();
		}

		@Override
		public Iterator<E> iterator() {
			return this.delegate.iterator();
		}

		@Override
		public Object[] toArray() {
			return this.delegate.toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return this.delegate.toArray(a);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return this.delegate.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			return this.delegate.addAll(c);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return this.delegate.removeAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return this.delegate.retainAll(c);
		}

		@Override
		public void clear() {
			this.delegate.clear();
		}

		@Override
		public void put(E e) throws InterruptedException {
			this.delegate.put(e);
		}

		@Override
		public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
			return this.delegate.offer(e, timeout, unit);
		}

		@Override
		public E take() throws InterruptedException {
			return this.delegate.take();
		}

		@Override
		public E poll(long timeout, TimeUnit unit) throws InterruptedException {
			return this.delegate.poll(timeout, unit);
		}

		@Override
		public int remainingCapacity() {
			return this.delegate.remainingCapacity();
		}

		@Override
		public boolean remove(Object o) {
			return this.delegate.remove(o);
		}

		@Override
		public boolean contains(Object o) {
			return this.delegate.contains(o);
		}

		@Override
		public int drainTo(Collection<? super E> c) {
			return this.delegate.drainTo(c);
		}

		@Override
		public int drainTo(Collection<? super E> c, int maxElements) {
			return this.delegate.drainTo(c, maxElements);
		}

		@Override
		public boolean tryTransfer(E e) {
			return this.delegate.tryTransfer(e);
		}

		@Override
		public void transfer(E e) throws InterruptedException {
			this.delegate.transfer(e);
		}

		@Override
		public boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException {
			return this.delegate.tryTransfer(e, timeout, unit);
		}

		@Override
		public boolean hasWaitingConsumer() {
			return this.delegate.hasWaitingConsumer();
		}

		@Override
		public int getWaitingConsumerCount() {
			return this.delegate.getWaitingConsumerCount();
		}
	}
}
