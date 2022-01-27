package pl.jakubweg.objects;

import pl.jakubweg.SponsorBlockSettings;

public class SponsorSegment implements Comparable<SponsorSegment> {
    public final long start;
    public final long end;
    public final SponsorBlockSettings.SegmentCategory category;
    public final String UUID;

    public SponsorSegment(long start, long end, SponsorBlockSettings.SegmentCategory category, String UUID) {
        this.start = start;
        this.end = end;
        this.category = category;
        this.UUID = UUID;
    }

    @Override
    public String toString() {
        return "SegmentCategory{" +
                "start=" + start +
                ", end=" + end +
                ", category='" + category + '\'' +
                '}';
    }

    @Override
    public int compareTo(SponsorSegment o) {
        return (int) (this.start - o.start);
    }
}
