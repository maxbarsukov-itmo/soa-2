import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Table } from 'react-bootstrap';
import axios from 'axios';
import ErrorHandler from './ErrorHandler';

// FIXME https
const DEMOGRAPHY_SERVICE_URL = 'http://localhost:51312/api/v1';

const DemographyStats = () => {
  const [hairColorStats, setHairColorStats] = useState([]);
  const [eyeColorStats, setEyeColorStats] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const hairColors = ['GREEN', 'RED', 'YELLOW', 'ORANGE', 'BROWN'];
  const eyeColors = ['RED', 'BLUE', 'YELLOW', 'ORANGE'];

  const fetchHairColorStats = async () => {
    const stats = [];
    for (const color of hairColors) {
      try {
        const response = await axios.get(`${DEMOGRAPHY_SERVICE_URL}/demography/hair-color/${color}/percentage`);
        stats.push({ color, percentage: parseFloat(response.data).toFixed(2) });
      } catch (err) {
        console.error(`Error fetching percentage for hair color ${color}:`, err);
        stats.push({ color, percentage: `Error: ${err.response?.data?.message || err.message}` });
      }
    }
    setHairColorStats(stats);
  };

  const fetchEyeColorStats = async () => {
    const stats = [];
    for (const color of eyeColors) {
      try {
        const response = await axios.get(`${DEMOGRAPHY_SERVICE_URL}/demography/eye-color/${color}`);
        stats.push({ color, count: response.data });
      } catch (err) {
        console.error(`Error fetching count for eye color ${color}:`, err);
        stats.push({ color, count: `Error: ${err.response?.data?.message || err.message}` });
      }
    }
    setEyeColorStats(stats);
  };

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      await Promise.all([fetchHairColorStats(), fetchEyeColorStats()]);
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleRefresh = () => {
    fetchData();
  };

  return (
    <Container>
      <Row>
        <Col>
          <Card>
            <Card.Header>
              <h2>Demographic Statistics</h2>
            </Card.Header>
            <Card.Body>
              <Button variant="primary" onClick={handleRefresh} disabled={loading}>
                {loading ? 'Loading...' : 'Refresh Stats'}
              </Button>
              {error && <ErrorHandler error={error} className="mt-3" />}
              <Row>
                <Col md={6}>
                  <Card className="mt-3">
                    <Card.Header>
                      <h4>Hair Color Percentage</h4>
                    </Card.Header>
                    <Card.Body>
                      <Table striped bordered hover responsive>
                        <thead>
                          <tr>
                            <th>Hair Color</th>
                            <th>Percentage (%)</th>
                          </tr>
                        </thead>
                        <tbody>
                          {hairColorStats.map((stat, index) => (
                            <tr key={index}>
                              <td>{stat.color}</td>
                              <td>{typeof stat.percentage === 'number' ? `${stat.percentage}%` : stat.percentage}</td>
                            </tr>
                          ))}
                        </tbody>
                      </Table>
                    </Card.Body>
                  </Card>
                </Col>
                <Col md={6}>
                  <Card className="mt-3">
                    <Card.Header>
                      <h4>Eye Color Count</h4>
                    </Card.Header>
                    <Card.Body>
                      <Table striped bordered hover responsive>
                        <thead>
                          <tr>
                            <th>Eye Color</th>
                            <th>Count</th>
                          </tr>
                        </thead>
                        <tbody>
                          {eyeColorStats.map((stat, index) => (
                            <tr key={index}>
                              <td>{stat.color}</td>
                              <td>{typeof stat.count === 'number' ? stat.count : stat.count}</td>
                            </tr>
                          ))}
                        </tbody>
                      </Table>
                    </Card.Body>
                  </Card>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default DemographyStats;
