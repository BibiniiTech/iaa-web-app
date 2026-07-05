'use client';

import React, { useRef, useState } from 'react';
import styles from '@/app/submissions/submissions.module.css';

interface FileUploadProps {
  files: File[];
  setFiles: (files: File[]) => void;
  maxFiles?: number;
  maxSizeMB?: number;
}

export default function FileUpload({ files, setFiles, maxFiles = 5, maxSizeMB = 25 }: FileUploadProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setError(null);
    if (e.target.files) {
      const newFiles = Array.from(e.target.files);
      const totalFiles = [...files, ...newFiles];

      if (totalFiles.length > maxFiles) {
        setError(`You can only upload a maximum of ${maxFiles} files.`);
        return;
      }

      const totalSize = totalFiles.reduce((acc, file) => acc + file.size, 0);
      if (totalSize > maxSizeMB * 1024 * 1024) {
        setError(`Total file size exceeds ${maxSizeMB}MB.`);
        return;
      }

      setFiles(totalFiles);
    }
  };

  const removeFile = (index: number) => {
    const updatedFiles = files.filter((_, i) => i !== index);
    setFiles(updatedFiles);
  };

  return (
    <div className={styles.uploadSection}>
      <p>Upload Files (Max {maxFiles}, Total {maxSizeMB}MB)</p>
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        style={{ display: 'none' }}
        multiple
      />
      <button
        type="button"
        className={styles.uploadButton}
        onClick={() => fileInputRef.current?.click()}
      >
        Select Files
      </button>

      {error && <p className={styles.error} style={{ marginTop: '0.5rem' }}>{error}</p>}

      {files.length > 0 && (
        <ul className={styles.fileList}>
          {files.map((file, index) => (
            <li key={index} className={styles.fileItem}>
              <span>{file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)</span>
              <button
                type="button"
                className={styles.removeFile}
                onClick={() => removeFile(index)}
              >
                ✕
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
