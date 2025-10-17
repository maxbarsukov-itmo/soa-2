import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import PeopleList from './PeopleList';
import DemographyStats from './DemographyStats';
import AdvancedSearch from './AdvancedSearch';
import BulkDeleteNationality from './BulkDeleteNationality';
import DeleteByLocation from './DeleteByLocation';
import LocationComparison from './LocationComparison';
import 'bootstrap/dist/css/bootstrap.min.css';

function App() {
  return (
    <Router>
      <div className="container-fluid">
        <nav className="navbar navbar-expand-lg navbar-light bg-light mb-4">
          <div className="container-fluid">
            <Link className="navbar-brand" to="/">SOA Client</Link>
            <div className="navbar-nav">
              <Link className="nav-link" to="/people">People Collection</Link>
              <Link className="nav-link" to="/advanced-search">Advanced Search</Link>
              <Link className="nav-link" to="/bulk-delete-nationality">Bulk Delete by Nationality</Link>
              <Link className="nav-link" to="/delete-by-location">Delete by Location</Link>
              <Link className="nav-link" to="/location-comparison">Location Comparison</Link>
              <Link className="nav-link" to="/demography">Demography Stats</Link>
            </div>
          </div>
        </nav>
        <Routes>
          <Route path="/" element={
            <div className="jumbotron">
              <h1 className="display-4">SOA Client Application</h1>
              <p className="lead">This client interacts with the People Collection Service and Demography Service.</p>
              <hr className="my-4" />
              <p>Navigate to the sections above to manage people or view demographic statistics.</p>
            </div>
          } />
          <Route path="/people/*" element={<PeopleList />} />
          <Route path="/advanced-search" element={<AdvancedSearch />} />
          <Route path="/bulk-delete-nationality" element={<BulkDeleteNationality />} />
          <Route path="/delete-by-location" element={<DeleteByLocation />} />
          <Route path="/location-comparison" element={<LocationComparison />} />
          <Route path="/demography" element={<DemographyStats />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
