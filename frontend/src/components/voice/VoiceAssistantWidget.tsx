import React, { useState, useEffect } from 'react';
import { useVoiceAssistant } from '../../context/VoiceAssistantContext';
import { useVoiceCommandHandler } from '../../hooks/useVoiceAssistant';
import '../../../src/components/voice/VoiceWidget.css';

interface VoiceWidgetState {
  isVisible: boolean;
  isExpanded: boolean;
  currentMessage: string;
}

export const VoiceAssistantWidget: React.FC = () => {
  const voiceContext = useVoiceAssistant();
  const { commandState, authRole } = useVoiceCommandHandler();
  const [widgetState, setWidgetState] = useState<VoiceWidgetState>({
    isVisible: true,
    isExpanded: false,
    currentMessage: '',
  });

  // Update current message based on state
  useEffect(() => {
    let message = '';

    if (voiceContext.isSpeaking) {
      message = 'Speaking...';
    } else if (voiceContext.isListening) {
      message = 'Listening...';
    } else if (voiceContext.error) {
      message = `Error: ${voiceContext.error}`;
    } else if (voiceContext.transcript) {
      message = `Heard: ${voiceContext.transcript}`;
    } else if (commandState === 'waiting_password') {
      message = 'Please say your password...';
    } else if (commandState === 'waiting_email') {
      message = 'Please say your email...';
    } else if (commandState === 'authenticating') {
      message = 'Verifying credentials...';
    } else {
      message = 'Click to activate voice assistant';
    }

    setWidgetState((prev) => ({ ...prev, currentMessage: message }));
  }, [voiceContext.isListening, voiceContext.isSpeaking, voiceContext.error, voiceContext.transcript, commandState]);

  const handleActivate = async () => {
    if (voiceContext.isListening) {
      voiceContext.stopListening();
    } else {
      voiceContext.clearTranscript();
      voiceContext.clearError();
      voiceContext.startListening();
      await voiceContext.speak("Hi buddy! How could I help you?");
    }
  };

  const toggleExpand = () => {
    setWidgetState((prev) => ({
      ...prev,
      isExpanded: !prev.isExpanded,
    }));
  };

  const getStatusIcon = () => {
    if (voiceContext.isSpeaking) {
      return <i className="bi bi-volume-up-fill" />;
    }
    if (voiceContext.isListening) {
      return <i className="bi bi-mic-fill voice-listening" />;
    }
    if (commandState === 'authenticating') {
      return <i className="bi bi-shield-check" />;
    }
    return <i className="bi bi-microphone" />;
  };

  const getStatusClass = () => {
    if (voiceContext.error) return 'voice-error';
    if (voiceContext.isSpeaking) return 'voice-speaking';
    if (voiceContext.isListening) return 'voice-listening';
    if (commandState === 'authenticating') return 'voice-authenticating';
    return '';
  };

  return (
    <div className={`voice-widget ${widgetState.isExpanded ? 'expanded' : 'collapsed'}`}>
      <div className={`voice-widget-button ${getStatusClass()}`} onClick={handleActivate}>
        <div className="voice-icon">
          {getStatusIcon()}
          <span className="female-voice-badge" title="Female Voice Assistant">
            <i className="bi bi-person-voice" />
          </span>
        </div>
        {widgetState.isExpanded && <span className="voice-button-text">Voice</span>}
      </div>

      {widgetState.isExpanded && (
        <div className="voice-widget-panel">
          <div className="voice-panel-header">
            <h3>Voice Assistant</h3>
            <button className="voice-close-btn" onClick={toggleExpand} title="Close">
              <i className="bi bi-x-lg" />
            </button>
          </div>

          <div className="voice-status-display">
            <div className={`voice-status-badge ${getStatusClass()}`}>
              {commandState === 'waiting_password' && 'Waiting for Password'}
              {commandState === 'waiting_email' && 'Waiting for Email'}
              {commandState === 'authenticating' && 'Authenticating...'}
              {commandState === 'processing' && 'Processing...'}
              {voiceContext.isListening && 'Listening...'}
              {voiceContext.isSpeaking && 'Speaking...'}
              {!voiceContext.isListening &&
                !voiceContext.isSpeaking &&
                commandState === 'idle' &&
                'Ready'}
            </div>
            {authRole && <div className="voice-auth-role">Role: {authRole}</div>}
          </div>

          {voiceContext.error && (
            <div className="voice-error-message">
              <i className="bi bi-exclamation-circle" /> {voiceContext.error}
            </div>
          )}

          <div className="voice-transcript-box">
            <div className="voice-transcript-label">Transcript:</div>
            <div className="voice-transcript-content">
              {voiceContext.transcript || widgetState.currentMessage || '(no input yet)'}
            </div>
          </div>

          <div className="voice-widget-actions">
            <button
              className={`voice-btn voice-btn-primary ${voiceContext.isListening ? 'active' : ''}`}
              onClick={handleActivate}
              disabled={voiceContext.isSpeaking}
            >
              <i className={`bi ${voiceContext.isListening ? 'bi-stop-circle-fill' : 'bi-mic-fill'}`} />
              {voiceContext.isListening ? 'Stop Listening' : 'Start Listening'}
            </button>

            {voiceContext.transcript && (
              <button className="voice-btn voice-btn-secondary" onClick={() => voiceContext.clearTranscript()}>
                <i className="bi bi-trash" /> Clear
              </button>
            )}
          </div>

          <div className="voice-commands-hint">
            <details>
              <summary>Available Commands</summary>
              <div className="voice-commands-list">
                <div className="command-group">
                  <strong>Login:</strong>
                  <ul>
                    <li>"I am admin buddy"</li>
                    <li>"I am manager buddy"</li>
                    <li>"I am compliance buddy"</li>
                    <li>"vendor login"</li>
                  </ul>
                </div>
                <div className="command-group">
                  <strong>Navigation:</strong>
                  <ul>
                    <li>"go to dashboard"</li>
                    <li>"open vendors"</li>
                    <li>"show rfqs"</li>
                  </ul>
                </div>
              </div>
            </details>
          </div>
        </div>
      )}

      {!widgetState.isExpanded && (
        <button className="voice-expand-btn" onClick={toggleExpand} title="Expand Voice Widget">
          <i className="bi bi-chevron-up" />
        </button>
      )}
    </div>
  );
};

export default VoiceAssistantWidget;
