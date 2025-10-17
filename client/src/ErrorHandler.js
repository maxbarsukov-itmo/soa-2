import React from 'react';
import { Alert } from 'react-bootstrap';

const ErrorHandler = ({ error, className = '' }) => {
  if (!error) return null;

  let message = error;
  let code = null;
  let time = null;

  if (typeof error === 'object' && error !== null) {
    code = error.code;
    message = error.message || message;
    time = error.time || time;
  }

  return (
    <Alert variant="danger" className={className}>
      <strong>Error:</strong> {message}
      {code !== null && <div><strong>Code:</strong> {code}</div>}
      {time && <div><strong>Time:</strong> {time}</div>}
    </Alert>
  );
};

export default ErrorHandler;
