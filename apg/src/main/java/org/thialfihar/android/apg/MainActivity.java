/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BaseActivity {

    static class LauncherIcon {
        final String text;
        final int imgId;
        final OnClickListener clickListener;

        public LauncherIcon(int imgId, String text, OnClickListener clickListener) {
            super();
            this.imgId = imgId;
            this.text = text;
            this.clickListener = clickListener;
        }
    }

    static class DashboardAdapter extends BaseAdapter {
        private Context mContext;
        private LauncherIcon[] mIcons;

        public DashboardAdapter(Context c, LauncherIcon[] icons) {
            mContext = c;
            mIcons = icons;
        }

        @Override
        public int getCount() {
            return mIcons.length;
        }

        @Override
        public LauncherIcon getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        static class ViewHolder {
            public ImageView icon;
            public TextView text;
        }

        // Create a new ImageView for each item referenced by the Adapter
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder holder;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

                v = vi.inflate(R.layout.dashboard_icon, null);
                v.setOnClickListener(mIcons[position].clickListener);
                holder = new ViewHolder();
                holder.text = (TextView) v.findViewById(R.id.dashboard_icon_text);
                holder.icon = (ImageView) v.findViewById(R.id.dashboard_icon_img);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }

            holder.icon.setImageResource(mIcons[position].imgId);
            holder.text.setText(mIcons[position].text);

            return v;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard);

        GridView gridview = (GridView) findViewById(R.id.dashboard_grid);
        gridview.setAdapter(new DashboardAdapter(this,
            new LauncherIcon[] {
                new LauncherIcon(R.drawable.key, "Encrypt",
                    new OnClickListener() {
                        public void onClick(View v) {
                            Intent intent = new Intent(MainActivity.this, EncryptActivity.class);
                            intent.setAction(Apg.Intent.ENCRYPT);
                            startActivity(intent);
                        }
                    }),
                new LauncherIcon(R.drawable.key, "Decrypt",
                    new OnClickListener() {
                        public void onClick(View v) {
                            Intent intent = new Intent(MainActivity.this, DecryptActivity.class);
                            intent.setAction(Apg.Intent.DECRYPT);
                            startActivity(intent);
                        }
                    }),
                new LauncherIcon(R.drawable.key, "Manage Keys",
                    new OnClickListener() {
                        public void onClick(View v) {
                            startActivity(new Intent(MainActivity.this, PublicKeyListActivity.class));
                        }
                    }),
                new LauncherIcon(R.drawable.key, "My Keys",
                    new OnClickListener() {
                        public void onClick(View v) {
                            startActivity(new Intent(MainActivity.this, SecretKeyListActivity.class));
                        }
                    }),
            }));

        // Hack to disable GridView scrolling
        gridview.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });

        if (!mPreferences.hasSeenHelp()) {
            showDialog(Id.dialog.help);
        }

        if (Apg.isReleaseVersion(this) && !mPreferences.hasSeenChangeLog(Apg.getVersion(this))) {
            showDialog(Id.dialog.change_log);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Id.dialog.change_log: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("Changes " + Apg.getFullVersion(this));
                LayoutInflater inflater =
                    (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.info, null);
                TextView message = (TextView) layout.findViewById(R.id.message);

                message.setText("Changes:\n" +
                                "* \n" +
                                "\n" +
                                "WARNING: be careful editing your existing keys, as they " +
                                "WILL be stripped of certificates right now.\n" +
                                "\n" +
                                "Also: key cross-certification is NOT supported, so signing " +
                                "with those keys will get a warning when the signature is " +
                                "checked.\n" +
                                "\n" +
                                "I hope APG continues to be useful to you, please send " +
                                "bug reports, feature wishes, feedback.");
                alert.setView(layout);

                alert.setCancelable(false);
                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                MainActivity.this.removeDialog(Id.dialog.change_log);
                                                mPreferences.setHasSeenChangeLog(
                                                        Apg.getVersion(MainActivity.this), true);
                                            }
                                        });

                return alert.create();
            }

            case Id.dialog.help: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle(R.string.title_help);

                LayoutInflater inflater =
                        (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.info, null);
                TextView message = (TextView) layout.findViewById(R.id.message);
                message.setText(R.string.text_help);

                TransformFilter packageNames = new TransformFilter() {
                    public final String transformUrl(final Matcher match, String url) {
                        String name = match.group(1).toLowerCase();
                        if (name.equals("astro")) {
                            return "com.metago.astro";
                        } else if (name.equals("k-9 mail")) {
                            return "com.fsck.k9";
                        } else {
                            return "org.openintents.filemanager";
                        }
                    }
                };

                Pattern pattern = Pattern.compile("(OI File Manager|ASTRO|K-9 Mail)");
                String scheme = "market://search?q=pname:";
                message.setAutoLinkMask(0);
                Linkify.addLinks(message, pattern, scheme, null, packageNames);

                alert.setView(layout);

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                MainActivity.this.removeDialog(Id.dialog.help);
                                                mPreferences.setHasSeenHelp(true);
                                            }
                                        });

                return alert.create();
            }

            default: {
                return super.onCreateDialog(id);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.manage_public_keys, 0, R.string.menu_managePublicKeys)
                .setIcon(android.R.drawable.ic_menu_manage);
        menu.add(0, Id.menu.option.manage_secret_keys, 1, R.string.menu_manageSecretKeys)
                .setIcon(android.R.drawable.ic_menu_manage);
        menu.add(2, Id.menu.option.preferences, 3, R.string.menu_preferences)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(2, Id.menu.option.key_server, 4, R.string.menu_keyServer)
                .setIcon(android.R.drawable.ic_menu_search);
        menu.add(3, Id.menu.option.about, 5, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(3, Id.menu.option.help, 6, R.string.menu_help)
                .setIcon(android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Id.menu.option.manage_public_keys: {
                startActivity(new Intent(this, PublicKeyListActivity.class));
                return true;
            }

            case Id.menu.option.manage_secret_keys: {
                startActivity(new Intent(this, SecretKeyListActivity.class));
                return true;
            }

            case Id.menu.option.help: {
                showDialog(Id.dialog.help);
                return true;
            }

            case Id.menu.option.key_server: {
                startActivity(new Intent(this, KeyServerQueryActivity.class));
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }
}