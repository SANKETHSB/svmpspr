import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';

interface VoiceAssistantContextType {
  isListening: boolean;
  isSpeaking: boolean;
  transcript: string;
  error: string | null;
  startListening: () => void;
  stopListening: () => void;
  speak: (text: string) => Promise<void>;
  clearTranscript: () => void;
  clearError: () => void;
}

const VoiceAssistantContext = createContext<VoiceAssistantContextType | undefined>(undefined);

export const VoiceAssistantProvider = ({ children }: { children: ReactNode }) => {
  const [isListening, setIsListening] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Initialize speech recognition and synthesis
  const SpeechRecognition = window.SpeechRecognition || (window as any).webkitSpeechRecognition;
  const recognition = new SpeechRecognition();

  // Configure recognition
  recognition.continuous = false;
  recognition.interimResults = true;
  recognition.lang = 'en-US';

  const startListening = useCallback(() => {
    setIsListening(true);
    setTranscript('');
    setError(null);

    recognition.onstart = () => {
      setIsListening(true);
    };

    recognition.onresult = (event: any) => {
      let interimTranscript = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          setTranscript((prev) => prev + transcript + ' ');
        } else {
          interimTranscript += transcript;
        }
      }
    };

    recognition.onerror = (event: any) => {
      setError(`Microphone error: ${event.error}`);
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognition.start();
  }, []);

  const stopListening = useCallback(() => {
    recognition.stop();
    setIsListening(false);
  }, []);

  const speak = useCallback(async (text: string): Promise<void> => {
    return new Promise((resolve) => {
      setIsSpeaking(true);

      const utterance = new SpeechSynthesisUtterance(text);

      // Get female voice - try to find a female voice in available voices
      const voices = window.speechSynthesis.getVoices();
      let femaleVoice = voices.find(
        (v) => v.name.toLowerCase().includes('female') || v.name.toLowerCase().includes('woman')
      );

      // Fallback to second voice if no female voice found
      if (!femaleVoice && voices.length > 1) {
        femaleVoice = voices[1]; // Usually female voice at index 1
      }

      if (femaleVoice) {
        utterance.voice = femaleVoice;
      }

      // Set voice parameters for cute, friendly tone
      utterance.pitch = 1.3; // Higher pitch for cute sound
      utterance.rate = 0.95; // Natural speed
      utterance.volume = 1.0;

      utterance.onend = () => {
        setIsSpeaking(false);
        resolve();
      };

      utterance.onerror = () => {
        setIsSpeaking(false);
        resolve();
      };

      window.speechSynthesis.speak(utterance);
    });
  }, []);

  const clearTranscript = useCallback(() => {
    setTranscript('');
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return (
    <VoiceAssistantContext.Provider
      value={{
        isListening,
        isSpeaking,
        transcript,
        error,
        startListening,
        stopListening,
        speak,
        clearTranscript,
        clearError,
      }}
    >
      {children}
    </VoiceAssistantContext.Provider>
  );
};

export const useVoiceAssistant = () => {
  const context = useContext(VoiceAssistantContext);
  if (!context) {
    throw new Error('useVoiceAssistant must be used within VoiceAssistantProvider');
  }
  return context;
};
