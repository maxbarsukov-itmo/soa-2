import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Table, Alert } from 'react-bootstrap';
import axios from 'axios';
import ErrorHandler from './ErrorHandler';

const PEOPLE_SERVICE_URL = 'http://localhost:8765/api/v1';

const AdvancedSearch = () => {
  const [filters, setFilters] = useState([{ field: '', operator: 'eq', value: '' }]);
  const [sortBy, setSortBy] = useState('');
  const [sortOrder, setSortOrder] = useState('asc');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [callbackUrl, setCallbackUrl] = useState('');

  const addFilter = () => {
    setFilters([...filters, { field: '', operator: 'eq', value: '' }]);
  };

  const updateFilter = (index, field, value) => {
    const newFilters = [...filters];
    newFilters[index][field] = value;
    setFilters(newFilters);
  };

  const removeFilter = (index) => {
    if (filters.length > 1) {
      const newFilters = [...filters];
      newFilters.splice(index, 1);
      setFilters(newFilters);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResults(null);

    try {
      const payload = { filters: filters.filter(f => f.field && f.value) }; // Убираем пустые фильтры
      const params = new URLSearchParams({
        sortBy: sortBy || undefined,
        sortOrder: sortOrder || undefined,
        page: page || undefined,
        pageSize: pageSize || undefined,
      }).toString();

      const url = `${PEOPLE_SERVICE_URL}/people/search${params ? '?' + params : ''}`;
      const headers = callbackUrl ? { 'X-Callback-URL': callbackUrl } : {};

      const response = await axios.post(url, payload, { headers });

      if (response.status === 202) {
        // Асинхронный запрос
        setResults({ message: 'Search task accepted. Check your callback URL for results.', taskId: response.data.taskId });
      } else {
        // Синхронный запрос
        setResults(response.data);
      }
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
              <h2>Advanced Search</h2>
            </Card.Header>
            <Card.Body>
              <Form onSubmit={handleSubmit}>
                {filters.map((filter, index) => (
                  <Row key={index} className="mb-2">
                    <Col md={4}>
                      <Form.Group controlId={`field-${index}`}>
                        <Form.Label>Field</Form.Label>
                        <Form.Select
                          value={filter.field}
                          onChange={(e) => updateFilter(index, 'field', e.target.value)}
                        >
                          <option value="">Select Field</option>
                          <option value="id">ID</option>
                          <option value="name">Name</option>
                          <option value="creationDate">Creation Date</option>
                          <option value="coordinates.x">Coordinates X</option>
                          <option value="coordinates.y">Coordinates Y</option>
                          <option value="height">Height</option>
                          <option value="eyeColor">Eye Color</option>
                          <option value="hairColor">Hair Color</option>
                          <option value="nationality">Nationality</option>
                          <option value="location.x">Location X</option>
                          <option value="location.y">Location Y</option>
                          <option value="location.z">Location Z</option>
                          <option value="location.name">Location Name</option>
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col md={2}>
                      <Form.Group controlId={`operator-${index}`}>
                        <Form.Label>Operator</Form.Label>
                        <Form.Select
                          value={filter.operator}
                          onChange={(e) => updateFilter(index, 'operator', e.target.value)}
                        >
                          <option value="eq">=</option>
                          <option value="ne">!=</option>
                          <option value="gt">&gt;</option>
                          <option value="lt">&lt;</option>
                          <option value="gte">&gte;</option>
                          <option value="lte">&lte;</option>
                        </Form.Select>
                      </Form.Group>
                    </Col>
                    <Col md={4}>
                      <Form.Group controlId={`value-${index}`}>
                        <Form.Label>Value</Form.Label>
                        <Form.Control
                          type="text"
                          value={filter.value}
                          onChange={(e) => updateFilter(index, 'value', e.target.value)}
                          placeholder="Value to compare"
                        />
                      </Form.Group>
                    </Col>
                    <Col md={2}>
                      <Form.Label>&nbsp;</Form.Label> {/* Spacer */}
                      <div>
                        <Button variant="danger" size="sm" onClick={() => removeFilter(index)}>
                          Remove
                        </Button>
                      </div>
                    </Col>
                  </Row>
                ))}
                <Button variant="secondary" size="sm" onClick={addFilter} className="mb-3">
                  Add Filter
                </Button>

                <Row>
                  <Col md={6}>
                    <Form.Group className="mb-3" controlId="sortBy">
                      <Form.Label>Sort By</Form.Label>
                      <Form.Select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
                        <option value="">None</option>
                        <option value="id">ID</option>
                        <option value="name">Name</option>
                        <option value="creationDate">Creation Date</option>
                        <option value="coordinates.x">Coordinates X</option>
                        <option value="coordinates.y">Coordinates Y</option>
                        <option value="height">Height</option>
                        <option value="eyeColor">Eye Color</option>
                        <option value="hairColor">Hair Color</option>
                        <option value="nationality">Nationality</option>
                        <option value="location.x">Location X</option>
                        <option value="location.y">Location Y</option>
                        <option value="location.z">Location Z</option>
                        <option value="location.name">Location Name</option>
                      </Form.Select>
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group className="mb-3" controlId="sortOrder">
                      <Form.Label>Sort Order</Form.Label>
                      <Form.Select value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}>
                        <option value="asc">Ascending</option>
                        <option value="desc">Descending</option>
                      </Form.Select>
                    </Form.Group>
                  </Col>
                </Row>

                <Row>
                  <Col md={3}>
                    <Form.Group className="mb-3" controlId="page">
                      <Form.Label>Page</Form.Label>
                      <Form.Control
                        type="number"
                        value={page}
                        onChange={(e) => setPage(parseInt(e.target.value))}
                        min="0"
                      />
                    </Form.Group>
                  </Col>
                  <Col md={3}>
                    <Form.Group className="mb-3" controlId="pageSize">
                      <Form.Label>Page Size</Form.Label>
                      <Form.Control
                        type="number"
                        value={pageSize}
                        onChange={(e) => setPageSize(parseInt(e.target.value))}
                        min="1"
                      />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group className="mb-3" controlId="callbackUrl">
                      <Form.Label>Callback URL (Optional)</Form.Label>
                      <Form.Control
                        type="url"
                        value={callbackUrl}
                        onChange={(e) => setCallbackUrl(e.target.value)}
                        placeholder="https://example.com/webhook"
                      />
                      <Form.Text muted>
                        If provided, search will be asynchronous.
                      </Form.Text>
                    </Form.Group>
                  </Col>
                </Row>

                <Button variant="primary" type="submit" disabled={loading}>
                  {loading ? 'Searching...' : 'Search'}
                </Button>
              </Form>
              {error && <ErrorHandler error={error} className="mt-3" />}
              {results && (
                <div className="mt-4">
                  <h4>Search Results</h4>
                  {results.message ? (
                    <Alert variant="info">
                      {results.message} {results.taskId && `Task ID: ${results.taskId}`}
                    </Alert>
                  ) : (
                    <>
                      <p>Total Count: {results.totalCount}</p>
                      <Table striped bordered hover responsive>
                        <thead>
                          <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Height</th>
                            <th>Eye Color</th>
                            <th>Hair Color</th>
                            <th>Nationality</th>
                            <th>Location</th>
                          </tr>
                        </thead>
                        <tbody>
                          {results.people && results.people.map(p => (
                            <tr key={p.id}>
                              <td>{p.id}</td>
                              <td>{p.name}</td>
                              <td>{p.height}</td>
                              <td>{p.eyeColor}</td>
                              <td>{p.hairColor}</td>
                              <td>{p.nationality}</td>
                              <td>{p.location ? `${p.location.x}, ${p.location.y}, ${p.location.z} (${p.location.name})` : 'N/A'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </Table>
                    </>
                  )}
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default AdvancedSearch;
