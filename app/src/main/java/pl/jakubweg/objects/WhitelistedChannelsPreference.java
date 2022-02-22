package pl.jakubweg.objects;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import fi.vanced.libraries.youtube.player.ChannelModel;
import fi.vanced.libraries.youtube.whitelisting.Whitelist;
import fi.vanced.libraries.youtube.whitelisting.WhitelistType;
import fi.vanced.utils.VancedUtils;

public class WhitelistedChannelsPreference extends DialogPreference {

    private ArrayList<ChannelModel> mEntries;
    private WhitelistType whitelistType;

    public WhitelistedChannelsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public WhitelistedChannelsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WhitelistedChannelsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WhitelistedChannelsPreference(Context context) {
        super(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        mEntries = Whitelist.getWhitelistedChannels(whitelistType);

        Context context = getContext();
        if (mEntries.isEmpty()) {
            TextView emptyView = new TextView(context);
            emptyView.setText("There are no whitelisted channels.");
            emptyView.setTextSize(18);
            builder.setView(emptyView);
        }
        else {
            builder.setView(getEntriesListView(context));
        }
    }

    private View getEntriesListView(Context context) {
        LinearLayout entriesContainer = new LinearLayout(context);
        entriesContainer.setOrientation(LinearLayout.VERTICAL);
        for (final ChannelModel entry : mEntries) {
            String author = entry.getAuthor();
            View entryView = getEntryView(context, author, v -> {
                entriesContainer.removeView(entriesContainer.findViewWithTag(author));
                mEntries.remove(entry);
            });
            entryView.setTag(author);
            entriesContainer.addView(entryView);
        }
        return entriesContainer;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mEntries != null) {
            Whitelist.updateWhitelist(whitelistType, mEntries, getContext());
        }
    }

    private View getEntryView(Context context, CharSequence entry,
                              View.OnClickListener onDeleteClickListener) {
        int horizontalPadding = 80;

        Drawable divider = context.getResources().getDrawable(android.R.drawable.divider_horizontal_dark);
        Drawable deleteIcon = context.getResources().getDrawable(VancedUtils.getIdentifier("ic_baseline_delete_24", "drawable"));

        LinearLayout.LayoutParams entryContainerParams = new LinearLayout.LayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        entryContainerParams.setMargins(horizontalPadding, 0, horizontalPadding, 0);

        LinearLayout.LayoutParams entryLabelLayoutParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        entryLabelLayoutParams.gravity = Gravity.CENTER;

        LinearLayout entryContainer = new LinearLayout(context);
        entryContainer.setOrientation(LinearLayout.HORIZONTAL);
        entryContainer.setDividerDrawable(divider);
        entryContainer.setLayoutParams(entryContainerParams);

        TextView entryLabel = new TextView(context);
        entryLabel.setText(entry);
        entryLabel.setLayoutParams(entryLabelLayoutParams);
        entryLabel.setTextSize(18);

        ImageButton deleteButton = new ImageButton(context);
        deleteButton.setImageDrawable(deleteIcon);
        deleteButton.setOnClickListener(onDeleteClickListener);
        deleteButton.setBackground(null);

        entryContainer.addView(entryLabel);
        entryContainer.addView(deleteButton);
        return entryContainer;
    }

    public void setWhitelistType(WhitelistType whitelistType) {
        this.whitelistType = whitelistType;
    }
}