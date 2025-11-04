package me.ryanhamshire.GriefPrevention.util;

import com.griefprevention.util.IntVector;

import java.util.Collection;

public class LineCuller {

    public static boolean shouldCull(Entry entry, Collection<Entry> entries) {
        // point entries need to be handled specially as they don't have a direction
        if (entry.from().equals(entry.to())) {
            IntVector p = entry.from();
            for (Entry other : entries) {
                if (entry == other)
                    continue;
                if (pointOnEntry(p, other.from(), other.to())) {
                    return true;
                }
            }
            return false;
        }

        // normal line containment check
        for (Entry other : entries) {
            if (entry == other)
                continue;
            if (canCompare(entry, other) && isContained(entry, other)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canCompare(Entry a, Entry b) {
        IntVector dir = a.direction();
        if (!dir.equals(b.direction()) && !dir.equals(new IntVector(-b.direction().x(), -b.direction().y(), -b.direction().z())))
            return false; // different orientation

        // ensure same plane perpendicular to direction
        IntVector aFrom = a.from();
        IntVector bFrom = b.from();
        if (dir.x() != 0) {
            return aFrom.y() == bFrom.y() && aFrom.z() == bFrom.z();
        } else if (dir.y() != 0) {
            return aFrom.x() == bFrom.x() && aFrom.z() == bFrom.z();
        } else if (dir.z() != 0) {
            return aFrom.x() == bFrom.x() && aFrom.y() == bFrom.y();
        }
        return false;
    }

    private static boolean isContained(Entry inner, Entry outer) {
        // must be collinear
        if (!areCollinear(inner, outer))
            return false;

        // if the inner is a single point
        if (inner.from().equals(inner.to())) {
            return pointOnEntry(inner.from(), outer.from(), outer.to());
        }

        // otherwise, both endpoints must lie on outer
        return pointOnEntry(inner.from(), outer.from(), outer.to())
                && pointOnEntry(inner.to(), outer.from(), outer.to());
    }

    private static boolean areCollinear(Entry a, Entry b) {
        IntVector aDir = a.direction();
        IntVector cross = aDir.cross(b.direction());

        // must be parallel
        if (cross.x() != 0 || cross.y() != 0 || cross.z() != 0)
            return false;

        // must lie on the same line
        IntVector diff = b.from().subtract(a.from());
        IntVector diffCross = diff.cross(aDir);
        return diffCross.x() == 0 && diffCross.y() == 0 && diffCross.z() == 0;
    }

    private static boolean pointOnEntry(IntVector p, IntVector a, IntVector b) {
        if (p.equals(a) || p.equals(b))
            return true;

        IntVector ab = b.subtract(a);
        IntVector ap = p.subtract(a);
        IntVector cross = ab.cross(ap);
        if (cross.x() != 0 || cross.y() != 0 || cross.z() != 0)
            return false; // not collinear

        int dot = ap.dot(ab);
        if (dot < 0)
            return false; // before a
        return dot <= ab.dot(ab); // at or within end
    }

    public record Entry(IntVector from, IntVector to, IntVector direction) {

        public static Entry of(IntVector from, IntVector to) {
            if (from.equals(to)) // it's a point, it has no direction
                return new Entry(from, to, new IntVector(0,0,0));

            // order must be consistent
            if (shouldSwap(from, to)) {
                IntVector tmp = from;
                from = to;
                to = tmp;
            }

            IntVector dir = to.subtract(from);
            int g = gcd3(Math.abs(dir.x()), Math.abs(dir.y()), Math.abs(dir.z()));
            dir = new IntVector(dir.x() / g, dir.y() / g, dir.z() / g);
            return new Entry(from, to, dir);
        }

        private static boolean shouldSwap(IntVector from, IntVector to) {
            if (from.x() != to.x())
                return from.x() > to.x();
            if (from.y() != to.y())
                return from.y() > to.y();
            return from.z() > to.z();
        }

        private static int gcd3(int a, int b, int c) {
            return gcd(gcd(a, b), c);
        }

        private static int gcd(int a, int b) {
            return b == 0 ? a : gcd(b, a % b);
        }

    }

}

