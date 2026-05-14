import React, { useState } from 'react';
import { toast } from 'react-toastify';

interface ExportButtonProps {
  label: string;
  icon?: string;
  onExport: () => Promise<any>;
  filename: string;
  variant?: 'primary' | 'secondary' | 'success';
  size?: 'sm' | 'md';
}

/**
 * AC #11: Secure download helper — triggers browser download from blob response.
 * AC #7: Filename passed in from caller following standard convention.
 */
export const triggerDownload = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob);
  const a   = document.createElement('a');
  a.href    = url;
  // AC #11: Sanitize filename on frontend too
  a.download = filename.replace(/[^a-zA-Z0-9._\-]/g, '_');
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
};

const ExportButton: React.FC<ExportButtonProps> = ({
  label, icon = 'bi-download', onExport, filename,
  variant = 'secondary', size = 'sm',
}) => {
  const [loading, setLoading] = useState(false);

  const handleClick = async () => {
    setLoading(true);
    try {
      const res = await onExport();
      triggerDownload(new Blob([res.data]), filename);
      toast.success(`${label} downloaded successfully`);
    } catch (err: any) {
      toast.error(err.response?.data?.message || `Export failed`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      className={`btn btn-${variant} btn-${size}`}
      onClick={handleClick}
      disabled={loading}
      title={label}
    >
      <i className={`bi ${loading ? 'bi-hourglass-split' : icon}`} style={{ marginRight: 4 }} />
      {loading ? 'Exporting...' : label}
    </button>
  );
};

export default ExportButton;
