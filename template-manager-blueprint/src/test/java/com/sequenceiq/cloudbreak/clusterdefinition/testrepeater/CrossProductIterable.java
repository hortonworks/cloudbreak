package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import java.util.Iterator;

import javax.validation.constraints.NotNull;

public class CrossProductIterable<A, B> implements Iterable<Tuple<A, B>> {

    private final Iterable<A> as;

    private final Iterable<B> bs;

    CrossProductIterable(Iterable<A> as, Iterable<B> bs) {
        this.as = as;
        this.bs = bs;
    }

    @Override
    @NotNull
    public Iterator<Tuple<A, B>> iterator() {
        return new CrossProductIterator(as, bs);
    }

    public class CrossProductIterator implements Iterator<Tuple<A, B>> {

        private final Iterable<B> bs;

        private final Iterator<A> aIter;

        private Iterator<B> bIter;

        private A currentA;

        private boolean hasCurrentA;

        CrossProductIterator(Iterable<A> as, Iterable<B> bs) {
            this.bs = bs;
            aIter = as.iterator();
            nextA();
        }

        private void nextA() {
            hasCurrentA = aIter.hasNext();
            if (hasCurrentA) {
                currentA = aIter.next();
            }
            bIter = bs.iterator();

        }

        @Override
        public boolean hasNext() {
            boolean bHasNext = bIter.hasNext();
            if (!bHasNext) {
                nextA();
            }
            return hasCurrentA && bIter.hasNext();
        }

        @Override
        public Tuple<A, B> next() {
            return new Tuple<>(currentA, bIter.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "Can't remove from this iterator");
        }

    }

}
