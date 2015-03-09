package com.gittest.proj.ui.activity;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.CATEGORY_BROWSABLE;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
import static android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static com.github.mobile.accounts.AccountConstants.ACCOUNT_TYPE;
import static com.github.mobile.accounts.AccountConstants.PROVIDER_AUTHORITY;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.ViewFinder;
import com.github.mobile.DefaultClient;
import com.gittest.proj.R;
import com.gittest.proj.R.id;
import com.gittest.proj.R.layout;
import com.gittest.proj.R.menu;
import com.gittest.proj.R.string;
import com.github.mobile.accounts.AccountUtils;
import com.github.mobile.accounts.AuthenticatedUserTask;
import com.github.mobile.persistence.AccountDataManager;
import com.github.mobile.ui.LightProgressDialog;
import com.github.mobile.ui.TextWatcherAdapter;
import com.github.mobile.util.ToastUtils;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockAccountAuthenticatorActivity;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;

import roboguice.util.RoboAsyncTask;

public class LoginActivity extends RoboSherlockAccountAuthenticatorActivity {

    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

    public static final String PARAM_USERNAME = "username";

    private static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";

    private static final String TAG = "LoginActivity";

    private static final long SYNC_PERIOD = 8L * 60L * 60L;

    private static void configureSyncFor(Account account) {
        Log.d(TAG, "Configuring account sync");

        ContentResolver.setIsSyncable(account, PROVIDER_AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, PROVIDER_AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, PROVIDER_AUTHORITY,
                new Bundle(), SYNC_PERIOD);
    }

    private static class AccountLoader extends
            AuthenticatedUserTask<List<User>> {

        @Inject
        private AccountDataManager cache;

        protected AccountLoader(Context context) {
            super(context);
        }

        @Override
        protected List<User> run(Account account) throws Exception {
            return cache.getOrgs(true);
        }
    }

    private AccountManager accountManager;

    private AutoCompleteTextView loginText;

    private EditText passwordText;

    private RoboAsyncTask<User> authenticationTask;

    private String authTokenType;

    private MenuItem loginItem;
    
    private Boolean confirmCredentials = false;

    private String password;

    protected boolean requestNewAccount = false;

    private String username;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.login);

        accountManager = AccountManager.get(this);

        ViewFinder finder = new ViewFinder(this);
        loginText = finder.find(id.et_login);
        passwordText = finder.find(id.et_password);
        
        ((Button) findViewById(R.id.btnLogin)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleLogin();
			}
		});

        final Intent intent = getIntent();
        username = intent.getStringExtra(PARAM_USERNAME);
        authTokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
        requestNewAccount = username == null;
        confirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS,
                false);

        TextView signupText = finder.find(id.tv_signup);
        signupText.setMovementMethod(LinkMovementMethod.getInstance());
        signupText.setText(Html.fromHtml(getString(string.signup_link)));

        if (!TextUtils.isEmpty(username)) {
            loginText.setText(username);
            loginText.setEnabled(false);
            loginText.setFocusable(false);
        }

        TextWatcher watcher = new TextWatcherAdapter() {

            @Override
            public void afterTextChanged(Editable gitDirEditText) {
                updateEnablement();
            }
        };
        loginText.addTextChangedListener(watcher);
        passwordText.addTextChangedListener(watcher);

        passwordText.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event != null && ACTION_DOWN == event.getAction()
                        && keyCode == KEYCODE_ENTER && loginEnabled()) {
                    handleLogin();
                    return true;
                } else
                    return false;
            }
        });

        passwordText.setOnEditorActionListener(new OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == IME_ACTION_DONE && loginEnabled()) {
                    handleLogin();
                    return true;
                }
                return false;
            }
        });

        CheckBox showPassword = finder.find(id.cb_show_password);
        showPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                int type = TYPE_CLASS_TEXT;
                if (isChecked)
                    type |= TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                else
                    type |= TYPE_TEXT_VARIATION_PASSWORD;
                int selection = passwordText.getSelectionStart();
                passwordText.setInputType(type);
                if (selection > 0)
                    passwordText.setSelection(selection);
            }
        });

        loginText.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,
                getEmailAddresses()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Finish task if valid account exists
        if (requestNewAccount) {
            Account existing = AccountUtils.getPasswordAccessibleAccount(this);
            if (existing != null && !TextUtils.isEmpty(existing.name)) {
                String password = AccountManager.get(this)
                        .getPassword(existing);
                if (!TextUtils.isEmpty(password))
                    finishLogin(existing.name, password);
            }
            return;
        }

        updateEnablement();
    }

    private boolean loginEnabled() {
        return !TextUtils.isEmpty(loginText.getText())
                && !TextUtils.isEmpty(passwordText.getText());
    }

    private void updateEnablement() {
        if (loginItem != null)
            loginItem.setEnabled(loginEnabled());
    }

    @Override
    public void startActivity(Intent intent) {
        if (intent != null && ACTION_VIEW.equals(intent.getAction()))
            intent.addCategory(CATEGORY_BROWSABLE);

        super.startActivity(intent);
    }

    public void handleLogin() {
        if (requestNewAccount)
            username = loginText.getText().toString();
        password = passwordText.getText().toString();

        final AlertDialog dialog = LightProgressDialog.create(this,
                string.login_activity_authenticating);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                if (authenticationTask != null)
                    authenticationTask.cancel(true);
            }
        });
        dialog.show();

        authenticationTask = new RoboAsyncTask<User>(this) {

            @Override
            public User call() throws Exception {
                GitHubClient client = new DefaultClient();
                client.setCredentials(username, password);
                User user = new UserService(client).getUser();

                Account account = new Account(user.getLogin(), ACCOUNT_TYPE);
                if (requestNewAccount) {
                    accountManager
                            .addAccountExplicitly(account, password, null);
                    configureSyncFor(account);
                    try {
                        new AccountLoader(LoginActivity.this).call();
                    } catch (IOException e) {
                        Log.d(TAG, "Exception loading organizations", e);
                    }
                } else
                    accountManager.setPassword(account, password);

                return user;
            }

            @Override
            protected void onException(Exception e) throws RuntimeException {
                dialog.dismiss();

                Log.d(TAG, "Exception requesting authenticated user", e);

                if (AccountUtils.isUnauthorized(e))
                    onAuthenticationResult(false);
                else
                    ToastUtils.show(LoginActivity.this, e,
                            string.connection_failed);
            }

            @Override
            public void onSuccess(User user) {
                dialog.dismiss();

                onAuthenticationResult(true);
            }
        };
        authenticationTask.execute();
    }

    protected void finishConfirmCredentials(boolean result) {
        final Account account = new Account(username, ACCOUNT_TYPE);
        accountManager.setPassword(account, password);

        final Intent intent = new Intent();
        intent.putExtra(KEY_BOOLEAN_RESULT, result);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void finishLogin(final String username, final String password) {
        final Intent intent = new Intent();
        intent.putExtra(KEY_ACCOUNT_NAME, username);
        intent.putExtra(KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
        if (ACCOUNT_TYPE.equals(authTokenType))
            intent.putExtra(KEY_AUTHTOKEN, password);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onAuthenticationResult(boolean result) {
        if (result) {
            if (!confirmCredentials)
                finishLogin(username, password);
            else
                finishConfirmCredentials(true);
        } else {
            if (requestNewAccount)
                ToastUtils.show(this, string.invalid_login_or_password);
            else
                ToastUtils.show(this, string.invalid_password);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case id.m_login:
            handleLogin();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu optionMenu) {
        getSupportMenuInflater().inflate(menu.login, optionMenu);
        loginItem = optionMenu.findItem(id.m_login);
        return true;
    }

    private List<String> getEmailAddresses() {
        final Account[] accounts = accountManager
                .getAccountsByType("com.google");
        final List<String> addresses = new ArrayList<String>(accounts.length);
        for (Account account : accounts)
            addresses.add(account.name);
        return addresses;
    }
}
