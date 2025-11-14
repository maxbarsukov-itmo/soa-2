import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Table, Alert } from 'react-bootstrap';
import axios from 'axios';
import ErrorHandler from './ErrorHandler';

const PEOPLE_SERVICE_URL = 'http://localhost:8765/api/v1';

const LocationComparison = () => {
  const [comparisonCoords, setComparisonCoords] = useState({ x: null, y: null, z: null });
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setComparisonCoords(prev => ({
      ...prev,
      [name]: value !== '' ? (name === 'y' ? parseInt(value) : parseInt(value)) : null
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (comparisonCoords.x === null || comparisonCoords.y === null || comparisonCoords.z === null) {
      setError('All comparison coordinates (x, y, z) are required.');
      return;
    }
    setLoading(true);
    setError(null);
    setResults(null);

    try {
      const params = new URLSearchParams({
        x: comparisonCoords.x,
        y: comparisonCoords.y,
        z: comparisonCoords.z,
      }).toString();

      const response = await axios.get(`${PEOPLE_SERVICE_URL}/people/location/greater?${params}`);
      setResults(response.data);
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
              <h2>Location Comparison</h2>
            </Card.Header>
            <Card.Body>
              <Form onSubmit={handleSubmit}>
                <p>Find people whose location coordinates (x, y, z) are all greater than the specified values.</p>
                <Row>
                  <Col md={4}>
                    <Form.Group className="mb-3" controlId="compX">
                      <Form.Label>Compare X Greater Than *</Form.Label>
                      <Form.Control
                        type="number"
                        name="x"
                        value={comparisonCoords.x ?? ''}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group className="mb-3" controlId="compY">
                      <Form.Label>Compare Y Greater Than *</Form.Label>
                      <Form.Control
                        type="number"
                        name="y"
                        value={comparisonCoords.y ?? ''}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group className="mb-3" controlId="compZ">
                      <Form.Label>Compare Z Greater Than *</Form.Label>
                      <Form.Control
                        type="number"
                        name="z"
                        value={comparisonCoords.z ?? ''}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                  </Col>
                </Row>
                <Button variant="primary" type="submit" disabled={loading}>
                  {loading ? 'Comparing...' : 'Find People'}
                </Button>
              </Form>
              {error && <ErrorHandler error={error} className="mt-3" />}
              {results && results.people && (
                <div className="mt-4">
                  <h4>Results ({results.totalCount} found)</h4>
                  <Table striped bordered hover responsive>
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Location X</th>
                        <th>Location Y</th>
                        <th>Location Z</th>
                        <th>Location Name</th>
                      </tr>
                    </thead>
                    <tbody>
                      {results.people.map(p => (
                        <tr key={p.id}>
                          <td>{p.id}</td>
                          <td>{p.name}</td>
                          <td>{p.location?.x}</td>
                          <td>{p.location?.y}</td>
                          <td>{p.location?.z}</td>
                          <td>{p.location?.name}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default LocationComparison;
