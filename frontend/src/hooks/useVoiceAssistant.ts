import { useCallback, useState, useEffect } from 'react';
import { useVoiceAssistant as useVoiceContext } from '../context/VoiceAssistantContext';
import { parseVoiceCommand, CommandResult } from '../utils/voiceCommandRouter';
import {
  detectRoleFromVoice,
  verifyAdminPassword,
  verifyManagerPassword,
  verifyCompliancePassword,
  verifyVendorEmail,
  VoiceAuthResult,
} from '../utils/voiceAuth';
import { autoFillField } from '../utils/formAutofill';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

type CommandState = 'idle' | 'listening' | 'processing' | 'authenticating' | 'waiting_password' | 'waiting_email';

export const useVoiceCommandHandler = () => {
  const voiceContext = useVoiceContext();
  const navigate = useNavigate();
  const { login } = useAuth();
  const [commandState, setCommandState] = useState<CommandState>('idle');
  const [lastCommand, setLastCommand] = useState<CommandResult | null>(null);
  const [authRole, setAuthRole] = useState<string | null>(null);

  // Handle incoming command
  const handleCommand = useCallback(
    async (transcript: string) => {
      if (!transcript.trim()) return;

      setCommandState('processing');
      const command = parseVoiceCommand(transcript);
      setLastCommand(command);

      console.log('[Voice] Command:', command);

      try {
        switch (command.type) {
          case 'admin_login':
            setCommandState('waiting_password');
            setAuthRole('ADMIN');
            await voiceContext.speak(command.message || 'What is your admin password?');
            break;

          case 'manager_login':
            setCommandState('waiting_password');
            setAuthRole('PROCUREMENT_MANAGER');
            await voiceContext.speak(command.message || 'What is your manager password?');
            break;

          case 'compliance_login':
            setCommandState('waiting_password');
            setAuthRole('COMPLIANCE_OFFICER');
            await voiceContext.speak(command.message || 'What is your compliance password?');
            break;

          case 'vendor_login':
            setCommandState('waiting_email');
            setAuthRole('VENDOR');
            await voiceContext.speak(command.message || 'Please provide your Gmail address.');
            break;

          case 'navigation':
            setCommandState('processing');
            await voiceContext.speak(`Navigating to ${command.target}.`);
            navigate(command.target || '/dashboard');
            setCommandState('idle');
            break;

          case 'form_action':
            setCommandState('processing');
            if (command.target && command.message) {
              autoFillField(command.target, command.message);
              await voiceContext.speak(`Filled ${command.target} field.`);
            }
            setCommandState('idle');
            break;

          case 'help':
            setCommandState('processing');
            await voiceContext.speak(
              'You can say: "I am admin buddy", "I am manager buddy", "I am compliance buddy", or "vendor login". Or navigate by saying "go to" followed by a page name.'
            );
            setCommandState('idle');
            break;

          default:
            setCommandState('processing');
            await voiceContext.speak(
              command.message || 'Sorry, I did not understand that. Please try again or say help.'
            );
            setCommandState('idle');
        }
      } catch (error) {
        console.error('[Voice] Error handling command:', error);
        setCommandState('idle');
      }
    },
    [voiceContext, navigate, login]
  );

  // Handle password verification
  const handlePasswordVerification = useCallback(
    async (passwordTranscript: string) => {
      if (!authRole) return;

      setCommandState('authenticating');

      try {
        let result: VoiceAuthResult;

        if (authRole === 'ADMIN') {
          result = await verifyAdminPassword(passwordTranscript);
        } else if (authRole === 'PROCUREMENT_MANAGER') {
          result = await verifyManagerPassword(passwordTranscript);
        } else if (authRole === 'COMPLIANCE_OFFICER') {
          result = await verifyCompliancePassword(passwordTranscript);
        } else {
          return;
        }

        if (result.success && result.user) {
          await voiceContext.speak(result.message);
          // Login user
          const authUser = {
            id: result.user.username,
            email: result.user.username,
            username: result.user.username,
            roleType: result.user.role,
            role: result.user.role,
            accessToken: result.user.accessToken,
          };
          login(authUser);
          navigate('/dashboard');
        } else {
          await voiceContext.speak(result.message || 'Authentication failed. Please try again.');
        }

        setCommandState('idle');
        setAuthRole(null);
      } catch (error) {
        console.error('[Voice] Error verifying password:', error);
        await voiceContext.speak('Error during authentication. Please try again.');
        setCommandState('idle');
        setAuthRole(null);
      }
    },
    [authRole, voiceContext, navigate, login]
  );

  // Handle email verification for vendor login
  const handleEmailVerification = useCallback(
    async (emailTranscript: string) => {
      if (!authRole) return;

      setCommandState('authenticating');

      try {
        const result = await verifyVendorEmail(emailTranscript);

        if (result.success && result.user) {
          await voiceContext.speak(result.message);
          // Login user
          const authUser = {
            id: result.user.username,
            email: result.user.email || result.user.username,
            username: result.user.username,
            roleType: 'VENDOR',
            role: 'VENDOR',
            accessToken: result.user.accessToken,
          };
          login(authUser);
          navigate('/dashboard');
        } else {
          await voiceContext.speak(result.message || 'Email not found. Please try again.');
        }

        setCommandState('idle');
        setAuthRole(null);
      } catch (error) {
        console.error('[Voice] Error verifying email:', error);
        await voiceContext.speak('Error during login. Please try again.');
        setCommandState('idle');
        setAuthRole(null);
      }
    },
    [authRole, voiceContext, navigate, login]
  );

  // Listen for transcript changes and process commands
  useEffect(() => {
    if (!voiceContext.transcript || voiceContext.transcript.trim().length === 0) {
      return;
    }

    const transcript = voiceContext.transcript.trim();

    if (commandState === 'waiting_password') {
      // Process password
      handlePasswordVerification(transcript);
      voiceContext.clearTranscript();
    } else if (commandState === 'waiting_email') {
      // Process email
      handleEmailVerification(transcript);
      voiceContext.clearTranscript();
    } else if (commandState === 'idle') {
      // Process regular command
      handleCommand(transcript);
      voiceContext.clearTranscript();
    }
  }, [
    voiceContext.transcript,
    commandState,
    handleCommand,
    handlePasswordVerification,
    handleEmailVerification,
    voiceContext,
  ]);

  return {
    commandState,
    lastCommand,
    authRole,
    handleCommand,
  };
};
