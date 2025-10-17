import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Table, Button, Pagination, Form, Card, Alert, Modal } from 'react-bootstrap';
import axios from 'axios';
import PersonForm from './PersonForm';
import ErrorHandler from './ErrorHandler';
import { Link } from 'react-router-dom';

// FIXME https
const PEOPLE_SERVICE_URL = 'http://localhost:51313/api/v1';

const PeopleList = () => {
  const [people, setPeople] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [sortBy, setSortBy] = useState('id');
  const [sortOrder, setSortOrder] = useState('asc');
  const [editingPerson, setEditingPerson] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [filters, setFilters] = useState({
    name: '',
    eyeColor: '',
    nationality: '',
    heightMin: '',
    heightMax: '',
  });
  const [viewingPerson, setViewingPerson] = useState(null);
  const [showViewModal, setShowViewModal] = useState(false);

  const fetchPeople = async () => {
    setLoading(true);
    setError(null);
    try {
      // Построение строки фильтрации для GET запроса (простые фильтры)
      let filterQuery = Object.entries(filters)
        .filter(([key, value]) => value !== '')
        .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
        .join('&');

      let url = `${PEOPLE_SERVICE_URL}/people?page=${currentPage}&pageSize=${pageSize}&sortBy=${sortBy}&sortOrder=${sortOrder}`;
      if (filterQuery) {
        url += `&${filterQuery}`;
      }

      const response = await axios.get(url);
      setPeople(response.data.people || []);
      setTotalPages(response.data.totalPages || 0);
      setTotalElements(response.data.totalCount || 0);
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPeople();
  }, [currentPage, pageSize, sortBy, sortOrder, filters]);

  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  const handlePageSizeChange = (eventKey) => {
    setPageSize(parseInt(eventKey));
    setCurrentPage(0); // Сброс на первую страницу при изменении размера
  };

  const handleSortChange = (field) => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(field);
      setSortOrder('asc');
    }
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
    setCurrentPage(0); // Сброс на первую страницу при изменении фильтра
  };

  const handleEdit = (person) => {
    setEditingPerson(person);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this person?')) {
      try {
        await axios.delete(`${PEOPLE_SERVICE_URL}/people/${id}`);
        fetchPeople(); // Обновить список после удаления
      } catch (err) {
        setError(err.response?.data?.message || err.message);
      }
    }
  };

  const handleSave = () => {
    setShowForm(false);
    setEditingPerson(null);
    fetchPeople(); // Обновить список после сохранения
  };

  const handleCancelForm = () => {
    setShowForm(false);
    setEditingPerson(null);
  };

  const handleCreate = () => {
    setEditingPerson(null);
    setShowForm(true);
  };

  const handleViewDetails = (person) => {
    setViewingPerson(person);
    setShowViewModal(true);
  };

  const handleCloseViewModal = () => {
    setShowViewModal(false);
    setViewingPerson(null);
  };

  const renderPagination = () => {
    if (totalPages <= 1) return null;

    const items = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    if (startPage > 1) {
      items.push(
        <Pagination.First key="first" onClick={() => handlePageChange(0)} disabled={currentPage === 0} />
      );
    }

    for (let number = startPage; number <= endPage; number++) {
      items.push(
        <Pagination.Item
          key={number}
          active={number - 1 === currentPage}
          onClick={() => handlePageChange(number - 1)}
        >
          {number}
        </Pagination.Item>
      );
    }

    if (endPage < totalPages) {
      items.push(
        <Pagination.Last key="last" onClick={() => handlePageChange(totalPages - 1)} disabled={currentPage === totalPages - 1} />
      );
    }

    return <Pagination>{items}</Pagination>;
  };

  return (
    <Container>
      <Row>
        <Col>
          <Card>
            <Card.Header>
              <h2>People Collection</h2>
            </Card.Header>
            <Card.Body>
              <Row className="mb-3">
                <Col md={6}>
                  <Button variant="primary" onClick={handleCreate} className="me-2">
                    Add Person
                  </Button>
                  <Link to="/advanced-search" className="btn btn-secondary me-2">Advanced Search</Link>
                  <Link to="/bulk-delete-nationality" className="btn btn-danger me-2">Bulk Delete (Nationality)</Link>
                  <Link to="/delete-by-location" className="btn btn-danger me-2">Delete by Location</Link>
                  <Link to="/location-comparison" className="btn btn-info">Location Comparison</Link>
                </Col>
                <Col md={6} className="d-flex justify-content-end">
                  <Form.Group controlId="pageSizeSelect" className="me-3">
                    <Form.Label>Page Size:</Form.Label>
                    <Form.Select value={pageSize} onChange={(e) => handlePageSizeChange(e.target.value)}>
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
              </Row>

              {/* Фильтры */}
              <Row className="mb-3">
                <Col md={3}>
                  <Form.Group controlId="filterName">
                    <Form.Label>Name</Form.Label>
                    <Form.Control
                      type="text"
                      name="name"
                      value={filters.name}
                      onChange={handleFilterChange}
                      placeholder="Filter by name"
                    />
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group controlId="filterEyeColor">
                    <Form.Label>Eye Color</Form.Label>
                    <Form.Select
                      name="eyeColor"
                      value={filters.eyeColor}
                      onChange={handleFilterChange}
                    >
                      <option value="">All</option>
                      <option value="RED">Red</option>
                      <option value="BLUE">Blue</option>
                      <option value="YELLOW">Yellow</option>
                      <option value="ORANGE">Orange</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group controlId="filterNationality">
                    <Form.Label>Nationality</Form.Label>
                    <Form.Select
                      name="nationality"
                      value={filters.nationality}
                      onChange={handleFilterChange}
                    >
                      <option value="">All</option>
                      <option value="CHINA">China</option>
                      <option value="INDIA">India</option>
                      <option value="ITALY">Italy</option>
                      <option value="NORTH_KOREA">North Korea</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group controlId="filterHeightMin">
                    <Form.Label>Height Min</Form.Label>
                    <Form.Control
                      type="number"
                      name="heightMin"
                      value={filters.heightMin}
                      onChange={handleFilterChange}
                      placeholder="Min"
                    />
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Group controlId="filterHeightMax">
                    <Form.Label>Height Max</Form.Label>
                    <Form.Control
                      type="number"
                      name="heightMax"
                      value={filters.heightMax}
                      onChange={handleFilterChange}
                      placeholder="Max"
                    />
                  </Form.Group>
                </Col>
              </Row>

              {error && <ErrorHandler error={error} />}

              {loading ? (
                <p>Loading...</p>
              ) : (
                <>
                  <Table striped bordered hover responsive>
                    <thead>
                      <tr>
                        <th onClick={() => handleSortChange('id')}>ID {sortBy === 'id' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th onClick={() => handleSortChange('name')}>Name {sortBy === 'name' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th onClick={() => handleSortChange('coordinates.x')}>Coord X {sortBy === 'coordinates.x' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th onClick={() => handleSortChange('coordinates.y')}>Coord Y {sortBy === 'coordinates.y' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th onClick={() => handleSortChange('height')}>Height {sortBy === 'height' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th onClick={() => handleSortChange('eyeColor')}>Eye Color {sortBy === 'eyeColor' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th onClick={() => handleSortChange('hairColor')}>Hair Color {sortBy === 'hairColor' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th onClick={() => handleSortChange('nationality')}>Nationality {sortBy === 'nationality' && (sortOrder === 'asc' ? '↑' : '↓')}</th>
                        <th>Location</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {people.map(person => (
                        <tr key={person.id}>
                          <td>{person.id}</td>
                          <td>{person.name}</td>
                          <td>{person.coordinates?.x}</td>
                          <td>{person.coordinates?.y}</td>
                          <td>{person.height}</td>
                          <td>{person.eyeColor}</td>
                          <td>{person.hairColor}</td>
                          <td>{person.nationality}</td>
                          <td>{person.location ? `${person.location.x}, ${person.location.y}, ${person.location.z} (${person.location.name})` : 'N/A'}</td>
                          <td>
                            <Button variant="outline-primary" size="sm" onClick={() => handleViewDetails(person)} className="me-1">View</Button>
                            <Button variant="outline-secondary" size="sm" onClick={() => handleEdit(person)} className="me-1">Edit</Button>
                            <Button variant="outline-danger" size="sm" onClick={() => handleDelete(person.id)}>Delete</Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                  <div className="d-flex justify-content-between align-items-center">
                    <div>
                      Showing {people.length > 0 ? currentPage * pageSize + 1 : 0} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} entries
                    </div>
                    {renderPagination()}
                  </div>
                </>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
      {showForm && (
        <Row>
          <Col>
            <Card className="mt-3">
              <Card.Header>
                <h3>{editingPerson ? 'Edit Person' : 'Add New Person'}</h3>
              </Card.Header>
              <Card.Body>
                <PersonForm
                  person={editingPerson}
                  onSave={handleSave}
                  onCancel={handleCancelForm}
                />
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
      <Modal show={showViewModal} onHide={handleCloseViewModal}>
        <Modal.Header closeButton>
          <Modal.Title>Person Details</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {viewingPerson && (
            <div>
              <p><strong>ID:</strong> {viewingPerson.id}</p>
              <p><strong>Name:</strong> {viewingPerson.name}</p>
              <p><strong>Coordinates:</strong> ({viewingPerson.coordinates?.x}, {viewingPerson.coordinates?.y})</p>
              <p><strong>Creation Date:</strong> {viewingPerson.creationDate}</p>
              <p><strong>Height:</strong> {viewingPerson.height || 'N/A'}</p>
              <p><strong>Eye Color:</strong> {viewingPerson.eyeColor}</p>
              <p><strong>Hair Color:</strong> {viewingPerson.hairColor || 'N/A'}</p>
              <p><strong>Nationality:</strong> {viewingPerson.nationality || 'N/A'}</p>
              <p><strong>Location:</strong> ({viewingPerson.location?.x}, {viewingPerson.location?.y}, {viewingPerson.location?.z}) - {viewingPerson.location?.name || 'N/A'}</p>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCloseViewModal}>
            Close
          </Button>
          {viewingPerson && (
            <Button variant="primary" onClick={() => { handleEdit(viewingPerson); handleCloseViewModal(); }}>
              Edit
            </Button>
          )}
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default PeopleList;
