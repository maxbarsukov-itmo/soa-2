import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert } from 'react-bootstrap';
import axios from 'axios';
import ErrorHandler from './ErrorHandler';

// FIXME https
const PEOPLE_SERVICE_URL = 'http://localhost:51313/api/v1';

const DeleteByLocation = () => {
  const [location, setLocation] = useState({ x: null, y: null, z: null });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setLocation(prev => ({
      ...prev,
      [name]: value !== '' ? (name === 'y' ? parseInt(value) : parseInt(value)) : null
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (location.x === null || location.y === null || location.z === null) {
      setError('All location coordinates (x, y, z) are required.');
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      await axios.delete(`${PEOPLE_SERVICE_URL}/people/location`, { data: location });
      setResult(`Successfully deleted one person at location: (${location.x}, ${location.y}, ${location.z})`);
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
              <h2>Delete One Person by Location</h2>
            </Card.Header>
            <Card.Body>
              <Form onSubmit={handleSubmit}>
                <Row>
                  <Col md={4}>
                    <Form.Group className="mb-3" controlId="locX">
                      <Form.Label>Location X *</Form.Label>
                      <Form.Control
                        type="number"
                        name="x"
                        value={location.x ?? ''}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group className="mb-3" controlId="locY">
                      <Form.Label>Location Y *</Form.Label>
                      <Form.Control
                        type="number"
                        name="y"
                        value={location.y ?? ''}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group className="mb-3" controlId="locZ">
                      <Form.Label>Location Z *</Form.Label>
                      <Form.Control
                        type="number"
                        name="z"
                        value={location.z ?? ''}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                  </Col>
                </Row>
                <Button variant="danger" type="submit" disabled={loading}>
                  {loading ? 'Deleting...' : 'Delete Person at Location'}
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

export default DeleteByLocation;
