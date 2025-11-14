import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert } from 'react-bootstrap';
import axios from 'axios';
import ErrorHandler from './ErrorHandler';

const PEOPLE_SERVICE_URL = 'http://localhost:8765/api/v1';

const BulkDeleteNationality = () => {
  const [nationality, setNationality] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nationality) {
      setError('Please select a nationality.');
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      await axios.delete(`${PEOPLE_SERVICE_URL}/people/nationality/${nationality}`);
      setResult(`Successfully deleted all people with nationality: ${nationality}`);
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container>
      <Row>
        <Col>
          <Card>
            <Card.Header>
              <h2>Bulk Delete by Nationality</h2>
            </Card.Header>
            <Card.Body>
              <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="nationality">
                  <Form.Label>Select Nationality to Delete All People:</Form.Label>
                  <Form.Select
                    value={nationality}
                    onChange={(e) => setNationality(e.target.value)}
                    required
                  >
                    <option value="">Choose...</option>
                    <option value="CHINA">China</option>
                    <option value="INDIA">India</option>
                    <option value="ITALY">Italy</option>
                    <option value="NORTH_KOREA">North Korea</option>
                  </Form.Select>
                </Form.Group>
                <Button variant="danger" type="submit" disabled={loading}>
                  {loading ? 'Deleting...' : 'Delete All People with Selected Nationality'}
                </Button>
              </Form>
              {error && <ErrorHandler error={error} className="mt-3" />}
              {result && <Alert variant="success" className="mt-3">{result}</Alert>}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default BulkDeleteNationality;
