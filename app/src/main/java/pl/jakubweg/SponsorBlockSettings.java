package pl.jakubweg;

import static pl.jakubweg.StringRef.sf;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SponsorBlockSettings {

    public static final String PREFERENCES_NAME = "sponsor-block";
    public static final String PREFERENCES_KEY_SHOW_TOAST_WHEN_SKIP = "show-toast";
    public static final String PREFERENCES_KEY_COUNT_SKIPS = "count-skips";
    public static final String PREFERENCES_KEY_UUID = "uuid";
    public static final String PREFERENCES_KEY_ADJUST_NEW_SEGMENT_STEP = "new-segment-step-accuracy";
    public static final String PREFERENCES_KEY_SPONSOR_BLOCK_ENABLED = "sb-enabled";
    public static final String PREFERENCES_KEY_SEEN_GUIDELINES = "sb-seen-gl";
    public static final String PREFERENCES_KEY_NEW_SEGMENT_ENABLED = "sb-new-segment-enabled";
    public static final String PREFERENCES_KEY_VOTING_ENABLED = "sb-voting-enabled";
    public static final String PREFERENCES_KEY_SKIPPED_SEGMENTS = "sb-skipped-segments";
    public static final String PREFERENCES_KEY_SKIPPED_SEGMENTS_TIME = "sb-skipped-segments-time";
    public static final String PREFERENCES_KEY_SHOW_TIME_WITHOUT_SEGMENTS = "sb-length-without-segments";
    public static final String PREFERENCES_KEY_CATEGORY_COLOR_SUFFIX = "_color";
    public static final String PREFERENCES_KEY_VIDEO_SEGMENTS_PREFIX = "video_";

    public static final SegmentBehaviour DEFAULT_BEHAVIOR = SegmentBehaviour.SKIP_AUTOMATICALLY;

    public static boolean isSponsorBlockEnabled = false;
    public static boolean seenGuidelinesPopup = false;
    public static boolean isAddNewSegmentEnabled = false;
    public static boolean isVotingEnabled = true;
    public static boolean showToastWhenSkippedAutomatically = true;
    public static boolean countSkips = true;
    public static boolean showTimeWithoutSegments = true;
    public static int adjustNewSegmentMillis = 150;
    public static String uuid = "<invalid>";
    public static String sponsorBlockUrlCategories = "[]";
    public static int skippedSegments;
    public static long skippedTime;

    @SuppressWarnings("unused")
    @Deprecated
    public SponsorBlockSettings(Context ignored) {
        Log.e("jakubweg.Settings", "Do not call SponsorBlockSettings constructor!");
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static void setSeenGuidelines(Context context) {
        SponsorBlockSettings.seenGuidelinesPopup = true;
        getPreferences(context).edit().putBoolean(PREFERENCES_KEY_SEEN_GUIDELINES, true).apply();
    }

    public static void update(Context context) {
        if (context == null) return;

        SharedPreferences preferences = getPreferences(context);
        isSponsorBlockEnabled = preferences.getBoolean(PREFERENCES_KEY_SPONSOR_BLOCK_ENABLED, isSponsorBlockEnabled);
        seenGuidelinesPopup = preferences.getBoolean(PREFERENCES_KEY_SEEN_GUIDELINES, seenGuidelinesPopup);

        if (!isSponsorBlockEnabled) {
            SkipSegmentView.hide();
            NewSegmentHelperLayout.hide();
            SponsorBlockUtils.hideShieldButton();
            SponsorBlockUtils.hideVoteButton();
            PlayerController.sponsorSegmentsOfCurrentVideo = null;
        } else { /*isAddNewSegmentEnabled*/
            SponsorBlockUtils.showShieldButton();
        }

        isAddNewSegmentEnabled = preferences.getBoolean(PREFERENCES_KEY_NEW_SEGMENT_ENABLED, isAddNewSegmentEnabled);
        if (!isAddNewSegmentEnabled) {
            NewSegmentHelperLayout.hide();
            SponsorBlockUtils.hideShieldButton();
        } else {
            SponsorBlockUtils.showShieldButton();
        }

        isVotingEnabled = preferences.getBoolean(PREFERENCES_KEY_VOTING_ENABLED, isVotingEnabled);
        if (!isVotingEnabled)
            SponsorBlockUtils.hideVoteButton();
        else
            SponsorBlockUtils.showVoteButton();

        Set<String> enabledCategories = new HashSet<>(SegmentCategory.valuesWithoutUnsubmitted().length);

        for (SegmentCategory category : SegmentCategory.values()) {
            String categoryKey = category.key;
            String categoryColor = preferences.getString(categoryKey + PREFERENCES_KEY_CATEGORY_COLOR_SUFFIX, SponsorBlockUtils.formatColorString(category.defaultColor));
            category.setColor(Color.parseColor(categoryColor));

            String behaviorKey = preferences.getString(categoryKey, DEFAULT_BEHAVIOR.key);
            SegmentBehaviour behaviour = SegmentBehaviour.byKey(behaviorKey);

            category.behaviour = behaviour;

            if (!behaviour.isDisabled() && category != SegmentCategory.UNSUBMITTED) {
                enabledCategories.add(categoryKey);
            }
        }

        sponsorBlockUrlCategories = "[%22" + TextUtils.join("%22,%22", enabledCategories) + "%22]";

        skippedSegments = preferences.getInt(PREFERENCES_KEY_SKIPPED_SEGMENTS, skippedSegments);
        skippedTime = preferences.getLong(PREFERENCES_KEY_SKIPPED_SEGMENTS_TIME, skippedTime);

        showToastWhenSkippedAutomatically = preferences.getBoolean(PREFERENCES_KEY_SHOW_TOAST_WHEN_SKIP, showToastWhenSkippedAutomatically);
        String tmp1 = preferences.getString(PREFERENCES_KEY_ADJUST_NEW_SEGMENT_STEP, null);
        if (tmp1 != null)
            adjustNewSegmentMillis = Integer.parseInt(tmp1);

        countSkips = preferences.getBoolean(PREFERENCES_KEY_COUNT_SKIPS, countSkips);
        showTimeWithoutSegments = preferences.getBoolean(PREFERENCES_KEY_SHOW_TIME_WITHOUT_SEGMENTS, showTimeWithoutSegments);

        uuid = preferences.getString(PREFERENCES_KEY_UUID, null);
        if (uuid == null) {
            uuid = (UUID.randomUUID().toString() +
                    UUID.randomUUID().toString() +
                    UUID.randomUUID().toString())
                    .replace("-", "");
            preferences.edit().putString(PREFERENCES_KEY_UUID, uuid).apply();
        }
    }

    public enum SegmentBehaviour {
        SKIP_AUTOMATICALLY("skip", 2, sf("skip_automatically"), true),
        MANUAL_SKIP("manual-skip", 1, sf("skip_showbutton"), false),
        SHOW("show", 0, sf("skip_show"), false),
        DISABLED("disabled", -1, sf("skip_disabled"), false);

        public final String key;
        public final int desktopKey;
        public final StringRef name;
        public final boolean skip;

        SegmentBehaviour(String key, int desktopKey, StringRef name, boolean skip) {
            this.key = key;
            this.desktopKey = desktopKey;
            this.name = name;
            this.skip = skip;
        }

        public boolean isDisabled() {
            return this == SegmentBehaviour.DISABLED;
        }

        public static SegmentBehaviour byKey(String key) {
            for (SegmentBehaviour behaviour : values()) {
                if (behaviour.key.equals(key)) {
                    return behaviour;
                }
            }
            return null;
        }

        public static SegmentBehaviour byDesktopKey(int desktopKey) {
            for (SegmentBehaviour behaviour : values()) {
                if (behaviour.desktopKey == desktopKey) {
                    return behaviour;
                }
            }
            return null;
        }
    }

    public enum SegmentCategory {
        SPONSOR("sponsor", sf("segments_sponsor"), sf("skipped_sponsor"), sf("segments_sponsor_sum"), null, 0xFF00d400),
        INTRO("intro", sf("segments_intermission"), sf("skipped_intermission"), sf("segments_intermission_sum"), null, 0xFF00ffff),
        OUTRO("outro", sf("segments_endcards"), sf("skipped_endcard"), sf("segments_endcards_sum"), null, 0xFF0202ed),
        INTERACTION("interaction", sf("segments_subscribe"), sf("skipped_subscribe"), sf("segments_subscribe_sum"), null, 0xFFcc00ff),
        SELF_PROMO("selfpromo", sf("segments_selfpromo"), sf("skipped_selfpromo"), sf("segments_selfpromo_sum"), null, 0xFFffff00),
        MUSIC_OFFTOPIC("music_offtopic", sf("segments_nomusic"), sf("skipped_nomusic"), sf("segments_nomusic_sum"), null, 0xFFff9900),
        PREVIEW("preview", sf("segments_preview"), sf("skipped_preview"), sf("segments_preview_sum"), null, 0xFF008fd6),
        HIGHLIGHT("poi_highlight", sf("segments_highlight"), sf("skipped_to_highlight"), sf("segments_highlight_sum"), null, 0xFFff1684),
        UNSUBMITTED("unsubmitted", StringRef.empty, sf("skipped_unsubmitted"), StringRef.empty, SegmentBehaviour.SKIP_AUTOMATICALLY, 0xFFFFFFFF);

        public final String key;
        public final StringRef skipMessage;
        public final StringRef description;
        public final Paint paint;
        public final int defaultColor;
        public int color;
        public SegmentBehaviour behaviour;

        private final StringRef titleRef;
        public String rawTitle;
        public CharSequence title;

        private static final SegmentCategory[] valuesWithoutUnsubmitted;

        static {
            SegmentCategory[] values = values();
            SegmentCategory[] withoutUnsubmitted = new SegmentCategory[values.length - 1];
            for (int i = 0; i < values.length; i++) {
                SegmentCategory category = values[i];
                if (category != SegmentCategory.UNSUBMITTED) {
                    withoutUnsubmitted[i] = category;
                }
            }
            valuesWithoutUnsubmitted = withoutUnsubmitted;
        }

        SegmentCategory(String key, StringRef titleRef, StringRef skipMessage, StringRef description, SegmentBehaviour behaviour, int defaultColor) {
            this.key = key;
            this.skipMessage = skipMessage;
            this.description = description;
            this.behaviour = behaviour;
            this.defaultColor = defaultColor;

            this.paint = new Paint();
            this.color = defaultColor;
            this.titleRef = titleRef;

            formatTitle();
        }

        public void setColor(int color) {
            color = color & 0xFFFFFF;
            this.color = color;
            paint.setColor(color);
            paint.setAlpha(255);
            formatTitle();
        }

        private void formatTitle() {
            this.rawTitle = String.format("<font color=\"#%06X\">⬤</font> %s", color, titleRef);
            this.title = Html.fromHtml(rawTitle);
        }

        public static SegmentCategory byCategoryKey(String key) {
            for (SegmentCategory category : values()) {
                if (category.key.equals(key)) {
                    return category;
                }
            }
            return null;
        }

        public static SegmentCategory[] valuesWithoutUnsubmitted() {
            return valuesWithoutUnsubmitted;
        }
    }
}
