/****************************************************************************
 * Copyright (C) 2018 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the Open eCard App.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.demo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.openecard.android.activation.OpeneCard;
import org.openecard.demo.R;
import org.openecard.demo.fragments.FailureFragment;
import org.openecard.demo.fragments.UserInfoFragment;
import org.openecard.mobile.activation.ActivationController;
import org.openecard.mobile.activation.ActivationResult;
import org.openecard.mobile.activation.ActivationResultCode;
import org.openecard.mobile.activation.ActivationSource;
import org.openecard.mobile.activation.ConfirmAttributeSelectionOperation;
import org.openecard.mobile.activation.ConfirmOldSetNewPasswordOperation;
import org.openecard.mobile.activation.ConfirmPasswordOperation;
import org.openecard.mobile.activation.ConfirmTwoPasswordsOperation;
import org.openecard.mobile.activation.ContextManager;
import org.openecard.mobile.activation.ControllerCallback;
import org.openecard.mobile.activation.EacControllerFactory;
import org.openecard.mobile.activation.EacInteraction;
import org.openecard.mobile.activation.NFCOverlayMessageHandler;
import org.openecard.mobile.activation.PinManagementControllerFactory;
import org.openecard.mobile.activation.PinManagementInteraction;
import org.openecard.mobile.activation.ServerData;
import org.openecard.mobile.activation.ServiceErrorResponse;
import org.openecard.mobile.activation.StartServiceHandler;
import org.openecard.mobile.ex.ApduExtLengthNotSupported;
import org.openecard.mobile.ex.NfcDisabled;
import org.openecard.mobile.ex.NfcUnavailable;
import org.openecard.mobile.ex.UnableToInitialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


public class PINManagementActivity extends AppCompatActivity {

    private static final Logger LOG = LoggerFactory.getLogger(PINManagementActivity.class);
	private View cancelBtn;

	private ActivationController actController;
	private OpeneCard oe;
	private ContextManager context;

	private PinManagementControllerFactory pinMgmtFactory;

	//
//    private final PinMgmtActivationHandler<PINManagementActivity> activationImpl;
//    private PINManagementGui pinMngGui;
//    private Button cancelBtn;
//
//    public PINManagementActivity() {
//        this.activationImpl = new ActivationImpl();
//    }
//
//
//    private class ActivationImpl extends PinMgmtActivationHandler<PINManagementActivity> {
//
//        ActivationImpl() {
//            super(PINManagementActivity.this);
//        }
//
//        @Override
//        public void onGuiIfaceSet(PINManagementGui gui) {
//            PINManagementActivity.this.pinMngGui = gui;
//            initPinChangeGui();
//        }
//
//
//
//        @Override
//        public void onAuthenticationFailure(ActivationResult result) {
//            LOG.info("Authentication failure: {}", result);
//
//            // show error
//            String errorMsg = buildErrorMsg(result);
//            showMessageFragment(errorMsg);
//        }
//
//        @Override
//        public void onAuthenticationInterrupted(ActivationResult result) {
//            LOG.info("Authentication interrupted: {}", result);
//
//            // show error message
//            String errorMsg = buildInterruptedMsg(result);
//            showMessageFragment(errorMsg);
//        }
//
//        @Nullable
//        @Override
//        public Dialog showCardRemoveDialog() {
//            return new AlertDialog.Builder(PINManagementActivity.this)
//                    .setTitle("Remove the Card")
//                    .setMessage("Please remove the identity card.")
//                    .setNeutralButton("Proceed", (dialog, which) -> dialog.dismiss())
//                    .create();
//        }
//    }
//
//
//    @Override
//    public void onBackPressed() {
//        activationImpl.cancelAuthentication();
//    }
//
//
//
//    ///
//    /// Callback handlers from Activity which have to be forwarded to the Activation implementation
//    ///
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        activationImpl.onStart();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        activationImpl.onStop();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        activationImpl.onPause();
//    }
//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	LOG.info("Creating");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);

        cancelBtn = findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(view -> {
			LOG.info("Cancel pressed");
			cancelBtn.setEnabled(false);
			cancelBtn.setClickable(false);

			showUserInfoFragmentWithMessage("Cancelling authentication...", false, true);
			showFailureFragment("The User cancelled the authentication procedure, please wait for the process to end.");
			if(actController != null) {
				actController.cancelAuthentication();
			}

        });
		showUserInfoFragmentWithMessage("Please wait...", false, true);
    }
	@Override
	protected void onStart() {
		LOG.info("Starting.");
		this.oe = OpeneCard.createInstance();
		this.context = oe.context(this);
		try {
			this.context.start(new StartServiceHandler() {
				@Override
				public void onSuccess(ActivationSource activationSource) {
					pinMgmtFactory = activationSource.pinManagementFactory();
					actController = pinMgmtFactory.create(new PINManagementActivity.PINMgmtControllerCallback(), new PINManagementActivity.PINMgmtInteractionImp());
				}

				@Override
				public void onFailure(ServiceErrorResponse serviceErrorResponse) {
					LOG.error("Could not start OeC-Framework: {}", serviceErrorResponse);
				}
			});
		} catch (UnableToInitialize unableToInitialize) {
			LOG.error("Exception during start: {}", unableToInitialize);
		} catch (NfcUnavailable nfcUnavailable) {
			LOG.error("Exception during start: {}", nfcUnavailable);
		} catch (NfcDisabled nfcDisabled) {
			LOG.error("Exception during start: {}", nfcDisabled);
		} catch (ApduExtLengthNotSupported apduExtLengthNotSupported) {
			LOG.error("Exception during start: {}", apduExtLengthNotSupported);
		}

		super.onStart();
	}

	private void showFailureFragment(String errorMessage) {
		FailureFragment fragment = new FailureFragment();
		fragment.setErrorMessage(errorMessage);

		runOnUiThread(() -> {
			cancelBtn.setVisibility(View.INVISIBLE);
		});

		// show ServerDataFragment
		LOG.debug("Replace fragment with FailureFragment.");
		getFragmentManager().beginTransaction()
				.replace(R.id.fragment, fragment).addToBackStack(null).commitAllowingStateLoss();

	}

    private void showUserInfoFragmentWithMessage(String msg, boolean showConfirmBtn, boolean showSpinner){
		if (findViewById(R.id.fragment) != null) {
			UserInfoFragment fragment = new UserInfoFragment();
			fragment.setWaitMessage(msg);
			fragment.setConfirmBtn(showConfirmBtn);
			fragment.setSpinner(showSpinner);
			fragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction()
					.replace(R.id.fragment, fragment).addToBackStack(null).commit();
		}
	}
	@Override
	protected void onNewIntent(Intent intent) {
		LOG.info("On new intent.");
		super.onNewIntent(intent);
		try {
			oe.onNewIntent(this, intent);
		} catch (ApduExtLengthNotSupported apduExtLengthNotSupported) {
			LOG.error("Exception during start: {}", apduExtLengthNotSupported);
		} catch (IOException e) {
			LOG.error("exception during start: {}", e);
		}

		showUserInfoFragmentWithMessage("Please wait...", false, true);

	}
	private class PINMgmtControllerCallback implements ControllerCallback {
		@Override
		public void onStarted() {
			LOG.debug("onStarted");

		}

		@Override
		public void onAuthenticationCompletion(ActivationResult activationResult) {
			LOG.debug("onAuthenticationCompletion");
			actController = null;
			if(activationResult != null) {
				LOG.debug("onAuthenticationSuccess Result={}", activationResult.getResultCode());
				LOG.debug("onAuthenticationSuccess ResultMinor={}", activationResult.getProcessResultMinor());

				if(activationResult.getResultCode()== ActivationResultCode.OK) {
					showUserInfoFragmentWithMessage("Success", true, false);
				}else{
					showUserInfoFragmentWithMessage("Fail - " + activationResult.getResultCode().toString(), true, false);
				}
			}
		}
	}

	private class PINMgmtInteractionImp implements PinManagementInteraction {

		@Override
		public void onPinChangeable(int i, ConfirmOldSetNewPasswordOperation confirmOldSetNewPasswordOperation) {
			LOG.debug("onPinChangeable");

		}

		@Override
		public void onCanRequired(ConfirmPasswordOperation confirmPasswordOperation) {
			LOG.debug("onCanRequired");

		}

		@Override
		public void onPinBlocked(ConfirmPasswordOperation confirmPasswordOperation) {
			LOG.debug("onPinBlocked");

		}

		@Override
		public void requestCardInsertion() {
			LOG.debug("requestCardInsertion");

		}

		@Override
		public void requestCardInsertion(NFCOverlayMessageHandler nfcOverlayMessageHandler) {
			LOG.debug("requestCardInsertion");

		}

		@Override
		public void onCardInteractionComplete() {
			LOG.debug("onCardInteractionComplete");

		}

		@Override
		public void onCardRecognized() {
			LOG.debug("onCardRecognized");

		}

		@Override
		public void onCardRemoved() {
			LOG.debug("onCardRemoved");

		}
	}

//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        activationImpl.onResume();
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        activationImpl.onNewIntent(intent);
//        // if you receive a nfc tag, disable the cancel button until the next fragment comes in
//        //disableCancel();
//
//        if (pinMngGui == null || findViewById(R.id.fragment) != null) {
//            // show UserInfoFragment
//            Fragment fragment = new UserInfoFragment();
//            cancelBtn.setVisibility(View.VISIBLE);
//            fragment.setArguments(getIntent().getExtras());
//            getFragmentManager().beginTransaction()
//                    .replace(R.id.fragment, fragment).addToBackStack(null).commit();
//        }
//    }
//
//    public void onPINIsRequired(PinStatus status) {
//		PINChangeFragment fragment = new PINChangeFragment();
//		fragment.setStatus(status);
//
//		// show PINChangeFragment
//		getFragmentManager().beginTransaction()
//				.replace(R.id.fragment, fragment).addToBackStack(null).commitAllowingStateLoss();
//	}
//
//	public void onCANIsRequired(boolean triedBefore) {
//        GenericInputFragment fragment = new CANInputFragment();
//
//        if (triedBefore) {
//            fragment.setMessage("The entered CAN was wrong, please try again.");
//        }
//
//        // show CANInput
//        getFragmentManager().beginTransaction()
//                .replace(R.id.fragment, fragment).addToBackStack(null).commitAllowingStateLoss();
//    }
//
//    public void onPUKIsRequired(boolean triedBefore) {
//        GenericInputFragment fragment = new PUKInputFragment();
//
//        if (triedBefore) {
//            fragment.setMessage("The entered PUK was wrong, please try again.");
//        }
//
//        // show PUKInput
//        getFragmentManager().beginTransaction()
//                .replace(R.id.fragment, fragment).addToBackStack(null).commitAllowingStateLoss();
//    }
//
//    public void enterCan(String can) {
//        try {
//            boolean canCorrect = pinMngGui.enterCan(can);
//            LOG.info("CAN correct: {}", canCorrect);
//
//            if (canCorrect) {
//				try {
//					onPINIsRequired(pinMngGui.getPinStatus());
//				} catch (InterruptedException ex) {
//					LOG.error(ex.getMessage(), ex);
//				}
//            } else {
//				onCANIsRequired(true);
//            }
//        } catch (InterruptedException ex) {
//            LOG.error(ex.getMessage(), ex);
//        }
//    }
//
//    public void enterPUK(String puk) {
//        try {
//            boolean pukCorrect = pinMngGui.unblockPin(puk);
//            LOG.info("PUK correct: {}", pukCorrect);
//
//            if (! pukCorrect){
//				onPUKIsRequired(true);
//            } else {
//				showMessageFragment("PIN was successful unblocked.");
//            }
//        } catch (InterruptedException ex) {
//            LOG.error(ex.getMessage(), ex);
//        }
//    }
//
//    public void changePin(String oldPin, String newPin) {
//        try {
//            LOG.info("Perform PIN change...");
//            boolean changeSuccessful = pinMngGui.changePin(oldPin, newPin);
//            LOG.info("PINChange was successful: {}", changeSuccessful);
//
//            if (! changeSuccessful) {
//                initPinChangeGui();
//            } else {
//				showMessageFragment("Your PIN was changed successfully.");
//                pinMngGui.cancel();
//            }
//        } catch (InterruptedException ex) {
//            LOG.error(ex.getMessage(), ex);
//        }
//    }
//
//    private void showMessageFragment(String msg) {
//        FailureFragment fragment = new FailureFragment();
//        fragment.setErrorMessage(msg);
//
//        runOnUiThread(() -> cancelBtn.setEnabled(false));
//
//        // show ServerDataFragment
//        getFragmentManager().beginTransaction()
//                .replace(R.id.fragment, fragment).addToBackStack(null).commitAllowingStateLoss();
//    }
//
//    private void initPinChangeGui() {
//        try {
//            final PinStatus pinStatus = pinMngGui.getPinStatus();
//            LOG.info("PIN status: {}", pinStatus);
//
//            if (pinStatus.isNormalPinEntry()) {
//				onPINIsRequired(pinStatus);
//            } else if (pinStatus.needsCan()) {
//				onCANIsRequired(false);
//            } else if (pinStatus.needsPuk()) {
//				onPUKIsRequired(false);
//            } else if (pinStatus.isDead()) {
//                String msg = String.format("PIN Status is '%s'.", pinStatus);
//				showMessageFragment(msg);
//                LOG.error(msg);
//                pinMngGui.cancel();
//            }
//        } catch (InterruptedException ex) {
//            LOG.error(ex.getMessage(), ex);
//        }
//    }
//
//
//    ///
//    /// methods for building error messages
//    ///
//
//    private String buildErrorMsg(ActivationResult result) {
//        String msg;
//        if (result.getErrorMessage() != null) {
//            String errorType = result.getResultCode().name();
//            String errorMsg = result.getErrorMessage();
//            msg = String.format("During PIN Management an error occurred (%s): %s.", errorType, errorMsg);
//        } else if (result.getResultCode() != null) {
//            String errorType = result.getResultCode().name();
//            msg = String.format("During PIN Management an unknown error occurred (%s).", errorType);
//        } else {
//            msg = "During PIN Management an unknown error occurred.";
//        }
//        return msg;
//    }
//
//    private String buildInterruptedMsg(ActivationResult result) {
//        String msg;
//        if (result.getErrorMessage() != null) {
//            String errorType = result.getResultCode().name();
//            String errorMsg = result.getErrorMessage();
//            msg = String.format("PIN Management was interrupted (%s): %s.", errorType, errorMsg);
//        } else if (result.getResultCode() != null) {
//            String errorType = result.getResultCode().name();
//            msg = String.format("PIN Management was interrupted by the user or implicitly by a shutdown of a " +
//                    "subsystem or the whole system (%s).", errorType);
//        } else {
//            msg = "PIN Management was interrupted by the user or implicitly by a shutdown of a subsystem or the whole system.";
//        }
//        return msg;
//    }
//
}
